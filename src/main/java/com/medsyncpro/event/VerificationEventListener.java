package com.medsyncpro.event;

import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.entity.VerificationStatus;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
import com.medsyncpro.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationEventListener {

    private final NotificationRepository notificationRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final SseEmitterService sseEmitterService;

    @Async
    @EventListener
    @Transactional
    public void handleUserSignup(UserSignupEvent event) {
        User user = event.getUser();
        log.info("Handling signup event for user {} with role {}", user.getEmail(), user.getRole());

        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setStatus(VerificationStatus.UNVERIFIED);
        request = verificationRequestRepository.save(request);

        Notification notification = new Notification();
        notification.setType("VERIFICATION_REQUEST");
        notification.setReferenceId(request.getId());
        notification.setTitle("New " + user.getRole() + " Signup");
        notification.setMessage(user.getEmail() + " has signed up and requires verification.");
        notification.setRecipientId(null);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        sseEmitterService.sendToAdmins("notification", Map.of(
                "type", "VERIFICATION_REQUEST",
                "title", notification.getTitle(),
                "message", notification.getMessage()));
    }
}
