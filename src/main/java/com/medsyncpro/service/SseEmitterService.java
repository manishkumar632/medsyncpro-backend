package com.medsyncpro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    // 30 minutes timeout (can be 0L for no timeout)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // userId → emitter
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // track admin users
    private final Set<UUID> adminUserIds = ConcurrentHashMap.newKeySet();

    /**
     * Register new emitter
     */
    public SseEmitter addEmitter(UUID userId, boolean isAdmin) {

        // Remove old emitter if exists
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Lifecycle callbacks
        emitter.onCompletion(() -> cleanup(userId));
        emitter.onTimeout(() -> cleanup(userId));
        emitter.onError(e -> {
            log.warn("SSE error for user: {}", userId, e);
            cleanup(userId);
        });

        emitters.put(userId, emitter);

        if (isAdmin) {
            adminUserIds.add(userId);
        }

        // Initial handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to user: {}", userId);
            cleanup(userId);
        }

        log.info("SSE connected: {} (admin: {})", userId, isAdmin);
        return emitter;
    }

    /**
     * Send event to single user
     */
    public void sendToUser(UUID userId, String eventName, Object data) {

        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("Failed to send SSE to user: {}", userId);
            emitter.completeWithError(e);
            cleanup(userId);
        }
    }

    /**
     * Broadcast event to all admins
     */
    public void sendToAdmins(String eventName, Object data) {

        for (UUID adminId : adminUserIds) {
            sendToUser(adminId, eventName, data);
        }
    }

    /**
     * Remove emitter manually
     */
    public void removeEmitter(UUID userId) {

        SseEmitter emitter = emitters.remove(userId);
        adminUserIds.remove(userId);

        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }

        log.debug("SSE removed: {}", userId);
    }

    /**
     * Internal cleanup method
     */
    private void cleanup(UUID userId) {
        emitters.remove(userId);
        adminUserIds.remove(userId);
        log.debug("SSE cleaned up: {}", userId);
    }
}