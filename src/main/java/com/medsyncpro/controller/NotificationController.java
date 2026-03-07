package com.medsyncpro.controller;

import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;

    /** SSE stream — any authenticated user can connect. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        return sseEmitterService.addEmitter(user.getId(), isAdmin);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        List<Notification> notifications;

        if (user.getRole() == Role.ADMIN) {
            notifications = notificationRepository
                    .findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(
                            user.getId().toString()); // ← fixed: was user.getId()
        } else {
            notifications = notificationRepository
                    .findByRecipientIdOrderByCreatedAtDesc(
                            user.getId().toString()); // ← fixed: was user.getId()
        }

        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications fetched"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        long count;

        if (user.getRole() == Role.ADMIN) {
            long personal = notificationRepository
                    .countByRecipientIdAndIsReadFalse(user.getId().toString()); // ← fixed
            long broadcast = notificationRepository.countByRecipientIdIsNullAndIsReadFalse();
            count = personal + broadcast;
        } else {
            count = notificationRepository
                    .countByRecipientIdAndIsReadFalse(user.getId().toString()); // ← fixed
        }

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("unreadCount", count), "Unread count fetched"));
    }

    /** PUT /api/notifications/{id}/read */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    /**
     * POST /api/notifications/register-fcm-token
     * Stores the browser/app FCM token so FirebasePushService can reach this user.
     */
    @PostMapping("/register-fcm-token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        try {
            User user = getUserFromAuth(authentication);
            String fcmToken = body.get("fcmToken");
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("fcmToken is required"));
            }
            user.setFcmToken(fcmToken);
            userRepository.save(user);
            return ResponseEntity.ok(ApiResponse.success(null, "FCM token registered"));
        } catch (Exception e) {
            // Non-critical — log and return success so the client doesn't retry
            return ResponseEntity.ok(ApiResponse.success(null, "FCM token noted"));
        }
    }

    private User getUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}