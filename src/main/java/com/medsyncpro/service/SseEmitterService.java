package com.medsyncpro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

    // userId → SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Track admin user IDs for broadcast
    private final Map<String, Boolean> adminUserIds = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(String userId, boolean isAdmin) {
        // Remove existing emitter if any
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for user: {}", userId);
            emitters.remove(userId);
            adminUserIds.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out for user: {}", userId);
            emitters.remove(userId);
            adminUserIds.remove(userId);
        });

        emitter.onError(e -> {
            log.debug("SSE emitter error for user: {}", userId);
            emitters.remove(userId);
            adminUserIds.remove(userId);
        });

        emitters.put(userId, emitter);
        if (isAdmin) {
            adminUserIds.put(userId, true);
        }

        // Send initial heartbeat
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE connection established\"}"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to user: {}", userId);
            emitters.remove(userId);
            adminUserIds.remove(userId);
        }

        log.info("SSE emitter added for user: {} (admin: {})", userId, isAdmin);
        return emitter;
    }

    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.debug("SSE event '{}' sent to user: {}", eventName, userId);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user: {}", userId);
                emitters.remove(userId);
                adminUserIds.remove(userId);
            }
        }
    }

    public void sendToAdmins(String eventName, Object data) {
        for (String adminId : adminUserIds.keySet()) {
            sendToUser(adminId, eventName, data);
        }
    }

    public void removeEmitter(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        adminUserIds.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Already completed or errored
            }
        }
    }
}
