package com.medsyncpro.event;

import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationStatus;
import com.medsyncpro.repository.DocumentTypeRepository;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.service.EmailService;
import com.medsyncpro.service.FirebasePushService;
import com.medsyncpro.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final FirebasePushService firebasePushService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DocumentTypeRepository documentTypeRepository;

    // ─── Doctor submitted → notify all admins ────────────────────────────────

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVerificationSubmitted(VerificationSubmittedEvent event) {
        User doctor = event.getSubmittedBy();

        String title = "New Verification Request";
        String message = String.format(
                "%s has submitted documents for verification. Please review.",
                doctor.getEmail());

        // 1. Persist — recipientId = null broadcasts to all admins
        saveNotification("VERIFICATION_REQUEST", doctor.getId().toString(),
                title, message, null);

        // 2. Real-time SSE push to every connected admin
        sseEmitterService.sendToAdmins("notification", Map.of(
                "type", "VERIFICATION_REQUEST",
                "title", title,
                "message", message,
                "doctorId", doctor.getId().toString(),
                "email", doctor.getEmail()));

        // 3. FCM push to admins who have a token
        pushToAllAdmins(title, message);

        log.info("[EVENT] VerificationSubmitted — doctor={}", doctor.getId());
    }

    // ─── Admin approved or rejected → notify + email the doctor ─────────────

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVerificationDecision(VerificationDecisionEvent event) {
        User doctor = event.getUser();
        boolean approved = event.getDecision() == VerificationStatus.VERIFIED;

        String title, message;
        if (approved) {
            title = "Verification Approved ✅";
            message = "Your documents have been verified. You can now access all platform features.";
        } else {
            title = "Verification Rejected ❌";
            message = "Your verification was rejected. Reason: "
                    + (event.getComments() != null ? event.getComments() : "No reason provided.");
        }

        // 1. In-app notification targeted at the doctor
        saveNotification("VERIFICATION_DECISION", doctor.getId().toString(),
                title, message, doctor.getId().toString());

        // 2. Real-time SSE push to the doctor (if connected)
        sseEmitterService.sendToUser(doctor.getId(), "notification", Map.of(
                "type", "VERIFICATION_DECISION",
                "title", title,
                "message", message,
                "status", event.getDecision().name()));

        // 3. FCM push if the doctor has a token
        pushToUser(doctor, title, message);

        // 4. Email —
        // • Approved: send a congratulations email
        // • Rejected: send a rejection email with the reason
        if (approved) {
            emailService.sendVerificationApprovedEmail(doctor.getEmail(), resolveDisplayName(doctor));
        } else {
            emailService.sendVerificationRejectedEmail(
                    doctor.getEmail(), resolveDisplayName(doctor), event.getComments());
        }

        log.info("[EVENT] VerificationDecision — doctor={} status={}",
                doctor.getId(), event.getDecision());
    }

    // ─── Admin requested document re-upload → notify + email the doctor ──────

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVerificationResubmitRequested(VerificationResubmitEvent event) {
        User doctor = event.getDoctor();
        List<String> typeCodes = event.getDocumentTypeCodes(); // e.g. ["MEDICAL_LICENSE"]
        String comment = event.getComment();

        // ── Resolve human-readable names for the notification message ─────────
        // e.g. "MEDICAL_LICENSE" → "Medical License"
        List<String> typeNames = resolveDocumentTypeNames(typeCodes);
        String docSummary = typeNames.isEmpty()
                ? "your documents"
                : String.join(", ", typeNames);

        String title = "⚠️ Document Re-upload Required";
        String message = "An admin has requested you to re-upload: " + docSummary + "."
                + (comment != null && !comment.isBlank() ? " Note: " + comment : "");

        // 1. In-app notification targeted at the doctor
        saveNotification("RESUBMIT_REQUESTED", doctor.getId().toString(),
                title, message, doctor.getId().toString());

        // 2. Real-time SSE push to the doctor (if connected)
        sseEmitterService.sendToUser(doctor.getId(), "notification", Map.of(
                "type", "RESUBMIT_REQUESTED",
                "title", title,
                "message", message,
                "status", VerificationStatus.RESUBMIT_REQUESTED.name(),
                "documentTypes", typeCodes));

        // 3. FCM push
        pushToUser(doctor, title, message);

        // 4. Email with the per-document breakdown and admin comment
        emailService.sendResubmitRequestEmail(
                doctor.getEmail(), resolveDisplayName(doctor), typeNames, comment);

        log.info("[EVENT] ResubmitRequested — doctor={} docs={}", doctor.getId(), typeCodes);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Saves a Notification row. Pass {@code recipientId = null} to broadcast to all
     * admins.
     */
    private void saveNotification(String type, String referenceId,
            String title, String message, String recipientId) {
        Notification n = new Notification();
        n.setType(type);
        n.setReferenceId(referenceId);
        n.setTitle(title);
        n.setMessage(message);
        n.setRecipientId(recipientId);
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    /** Sends an FCM push to a single user if they have a token. */
    private void pushToUser(User user, String title, String message) {
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            try {
                firebasePushService.sendPushNotification(user.getFcmToken(), title, message);
            } catch (Exception e) {
                log.warn("[FCM] Failed push to user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /** Sends an FCM push to every admin who has a registered token. */
    private void pushToAllAdmins(String title, String message) {
        List<User> admins = userRepository
                .findByRoleAndDeletedFalse(Role.ADMIN, Pageable.unpaged())
                .getContent();
        for (User admin : admins) {
            pushToUser(admin, title, message);
        }
    }

    private String resolveDisplayName(User user) {
        String email = user.getEmail();
        return email != null ? email.split("@")[0] : "Doctor";
    }

    private List<String> resolveDocumentTypeNames(List<String> codes) {
        if (codes == null || codes.isEmpty())
            return List.of();

        return codes.stream().map(code -> {
            // Try DB lookup first
            return documentTypeRepository.findByCode(code)
                    .map(dt -> dt.getName())
                    .orElseGet(() ->
            // Fallback: "MEDICAL_LICENSE" → "Medical License"
            capitaliseWords(code.replace("_", " ").toLowerCase()));
        }).collect(Collectors.toList());
    }

    private String capitaliseWords(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}