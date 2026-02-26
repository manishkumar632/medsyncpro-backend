package com.medsyncpro.event;

import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
import com.medsyncpro.service.FirebasePushService;
import com.medsyncpro.service.SseEmitterService;
import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.entity.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationEventListener {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FirebasePushService firebasePushService;
    private final VerificationRequestRepository verificationRequestRepository;
    private final SseEmitterService sseEmitterService;

    @Async
    @EventListener
    @Transactional
    public void handleUserSignup(UserSignupEvent event) {
        User user = event.getUser();
        log.info("Handling signup event for user {} with role {}", user.getEmail(), user.getRole());

        // 1. Create Verification Request
        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setStatus(VerificationStatus.UNVERIFIED);
        request = verificationRequestRepository.save(request);

        // 2. Create Notification for Admins (recipientId = null means broadcast to all admins)
        Notification notification = new Notification();
        notification.setType("VERIFICATION_REQUEST");
        notification.setReferenceId(request.getId());
        notification.setTitle("New " + user.getRole() + " Signup");
        notification.setMessage(user.getName() + " has signed up and requires verification.");
        notification.setRecipientId(null); 
        notificationRepository.save(notification);

        // 3. Send SSE event to all connected admins
        sseEmitterService.sendToAdmins("notification", Map.of(
                "type", "VERIFICATION_REQUEST",
                "title", notification.getTitle(),
                "message", notification.getMessage()
        ));

        // 4. Trigger Firebase notification
        log.info("🔔 Would send FCM Push Notification to Admin Topic here: New {} requires verification", user.getRole());
    }

    @Async
    @EventListener
    @Transactional
    public void onDocumentSubmitted(DocumentSubmittedEvent event) {
        User user = event.getUser();
        String title = "Verification Documents Submitted";
        String message = "Dr. " + user.getName() + " has submitted documents for professional verification.";

        log.info("Handling DocumentSubmittedEvent for user: {}", user.getEmail());

        // Notify all admins
        List<User> admins = userRepository.findByRoleAndDeletedFalse(Role.ADMIN, org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        for (User admin : admins) {
            // Save notification in DB
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType("VERIFICATION_REQUEST");
            notification.setRecipientId(admin.getId());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Send Push Notification if Admin has an FCM Token
            if (admin.getFcmToken() != null && !admin.getFcmToken().isBlank()) {
                firebasePushService.sendPushNotification(admin.getFcmToken(), title, message);
            }
        }

        // Send SSE event to all connected admins (broadcast)
        sseEmitterService.sendToAdmins("notification", Map.of(
                "type", "VERIFICATION_REQUEST",
                "title", title,
                "message", message
        ));
    }

    @Async
    @EventListener
    @Transactional
    public void onVerificationDecision(VerificationDecisionEvent event) {
        User user = event.getUser();
        String status = event.getDecision().name();
        String title = "Verification Update";
        String message = "Your professional verification status is now: " + status;
        
        if (event.getComments() != null && !event.getComments().isBlank()) {
            message += ". Notes: " + event.getComments();
        }

        log.info("Handling VerificationDecisionEvent for user: {} to status: {}", user.getEmail(), status);

        // Save notification in DB
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType("VERIFICATION_DECISION");
        notification.setRecipientId(user.getId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Send SSE event to user for realtime badge update
        sseEmitterService.sendToUser(user.getId(), "notification", Map.of(
                "type", "VERIFICATION_DECISION",
                "title", title,
                "message", message,
                "verificationStatus", status
        ));

        // Send Push Notification if User has an FCM Token
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            firebasePushService.sendPushNotification(user.getFcmToken(), title, message);
        }
    }
}
