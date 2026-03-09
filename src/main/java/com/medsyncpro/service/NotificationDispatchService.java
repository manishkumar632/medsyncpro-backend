package com.medsyncpro.service;

import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final FirebasePushService firebasePushService;
    private final EmailService emailService;

    public void notifyUser(
            User recipient,
            String type,
            String title,
            String message,
            String referenceId,
            boolean email,
            boolean push) {
        notifyUser(recipient, type, title, message, referenceId, true, email, push);
    }

    public void notifyUser(
            User recipient,
            String type,
            String title,
            String message,
            String referenceId,
            boolean inApp,
            boolean email,
            boolean push) {

        if (recipient == null) {
            return;
        }

        if (inApp) {
            Notification notification = new Notification();
            notification.setRecipientId(recipient.getId().toString());
            notification.setType(type);
            notification.setReferenceId(referenceId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setIsRead(false);
            notificationRepository.save(notification);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("title", title);
            payload.put("message", message);
            if (referenceId != null) {
                payload.put("referenceId", referenceId);
            }
            sseEmitterService.sendToUser(recipient.getId(), "notification", payload);
        }

        if (push && recipient.getFcmToken() != null && !recipient.getFcmToken().isBlank()) {
            try {
                firebasePushService.sendPushNotification(recipient.getFcmToken(), title, message);
            } catch (Exception e) {
                log.warn("Failed to send push notification to {}: {}", recipient.getId(), e.getMessage());
            }
        }

        if (email && recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            String displayName = recipient.getEmail().contains("@")
                    ? recipient.getEmail().split("@")[0]
                    : recipient.getEmail();
            emailService.sendGenericNotificationEmail(recipient.getEmail(), displayName, title, message);
        }
    }

    public void notifyUsers(
            Collection<User> recipients,
            String type,
            String title,
            String message,
            String referenceId,
            boolean email,
            boolean push) {
        notifyUsers(recipients, type, title, message, referenceId, true, email, push);
    }

    public void notifyUsers(
            Collection<User> recipients,
            String type,
            String title,
            String message,
            String referenceId,
            boolean inApp,
            boolean email,
            boolean push) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (User recipient : recipients) {
            notifyUser(recipient, type, title, message, referenceId, inApp, email, push);
        }
    }

    public void notifyAdminBroadcast(String type, String title, String message, String referenceId) {
        Notification notification = new Notification();
        notification.setRecipientId(null);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("title", title);
        payload.put("message", message);
        if (referenceId != null) {
            payload.put("referenceId", referenceId);
        }
        sseEmitterService.sendToAdmins("notification", payload);
    }
}
