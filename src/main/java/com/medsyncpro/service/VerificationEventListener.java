package com.medsyncpro.service;

import com.medsyncpro.entity.*;
import com.medsyncpro.event.UserSignupEvent;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationEventListener {

    private final VerificationRequestRepository verificationRequestRepository;
    private final NotificationRepository notificationRepository;
    // Injecting FirebasePushService to notify admins immediately if needed 
    // Usually admins would subscribe to a topic, or we loop through admins
    // For simplicity, we just save the notification to the DB for the dashboard poll/query.
    // Given the prompt suggests triggering an admin notification event using FCM, 
    // ideally we'd broadcast to an "admins" topic in Firebase.

    @EventListener
    @Async // Requires @EnableAsync in the application class to run asynchronously 
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

        // 3. Trigger Firebase notification (Mocking 'admins' topic send for now)
        log.info("🔔 Would send FCM Push Notification to Admin Topic here: New {} requires verification", user.getRole());
        // firebasePushService.sendTopicNotification("admins", notification.getTitle(), notification.getMessage());
    }
}
