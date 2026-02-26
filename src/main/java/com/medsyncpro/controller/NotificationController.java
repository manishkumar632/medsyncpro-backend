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

    /**
     * SSE stream endpoint — any authenticated user can connect.
     * Sends realtime notification events.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        return sseEmitterService.addEmitter(user.getId(), isAdmin);
    }

    /**
     * GET /api/notifications — list notifications for current user.
     * Admins see admin-targeted + broadcast notifications.
     * Other users see notifications targeted to them.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        List<Notification> notifications;

        if (user.getRole() == Role.ADMIN) {
            notifications = notificationRepository.findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(user.getId());
        } else {
            notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        }

        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications fetched"));
    }

    /**
     * GET /api/notifications/unread-count — unread count for current user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        long count;

        if (user.getRole() == Role.ADMIN) {
            long personalCount = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
            long broadcastCount = notificationRepository.countByRecipientIdIsNullAndIsReadFalse();
            count = personalCount + broadcastCount;
        } else {
            count = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count fetched"));
    }

    /**
     * PUT /api/notifications/{id}/read — mark a notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    private User getUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }
        return user;
    }
}
