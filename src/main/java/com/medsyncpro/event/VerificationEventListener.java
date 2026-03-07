package com.medsyncpro.event;

import com.medsyncpro.entity.Appointment;
import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationEventListener {

    private final NotificationRepository notificationRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final SseEmitterService sseEmitterService;

    // ─── New user signed up → create UNVERIFIED request + notify admins ──────

    @Async
    @EventListener
    @Transactional
    public void handleUserSignup(UserSignupEvent event) {
        User user = event.getUser();
        log.info("Handling signup event for user {} with role {}", user.getEmail(), user.getRole());

        // 1. Create initial verification request
        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setStatus(VerificationStatus.UNVERIFIED);
        request = verificationRequestRepository.save(request);

        // 2. Broadcast notification to all admins
        Notification notification = new Notification();
        notification.setType("VERIFICATION_REQUEST");
        notification.setReferenceId(request.getId());
        notification.setTitle("New " + user.getRole() + " Signup");
        notification.setMessage(user.getEmail() + " has signed up and requires verification.");
        notification.setRecipientId(null); // null = broadcast to all admins
        notificationRepository.save(notification);

        // 3. SSE push to connected admins
        sseEmitterService.sendToAdmins("notification", Map.of(
                "type", "VERIFICATION_REQUEST",
                "title", notification.getTitle(),
                "message", notification.getMessage()));
    }

    // ─── Appointment booked → notify doctor ──────────────────────────────────

    @Async
    @EventListener
    @Transactional
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        Appointment appointment = event.getAppointment();
        User doctorUser = appointment.getDoctor().getUser();
        String patientName = appointment.getPatient().getName();

        String title = "New Appointment Booking";
        String message = patientName + " has booked an appointment for "
                + appointment.getScheduledDate() + " at " + appointment.getScheduledTime()
                + " (" + appointment.getType() + ")";

        log.info("AppointmentBookedEvent: patient {} booked with doctor {}",
                patientName, doctorUser.getEmail());

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType("APPOINTMENT_BOOKED");
        notification.setRecipientId(String.valueOf(doctorUser.getId()));
        notification.setReferenceId(appointment.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        sseEmitterService.sendToUser(doctorUser.getId(), "notification", Map.of(
                "type", "APPOINTMENT_BOOKED",
                "title", title,
                "message", message));
    }

    // ─── Appointment cancelled → notify the other party ──────────────────────

    @Async
    @EventListener
    @Transactional
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        Appointment appointment = event.getAppointment();
        String cancelledBy = event.getCancelledBy();

        User recipientUser;
        String cancellerName;

        if ("PATIENT".equals(cancelledBy)) {
            recipientUser = appointment.getDoctor().getUser();
            cancellerName = appointment.getPatient().getName();
        } else {
            recipientUser = appointment.getPatient().getUser();
            cancellerName = appointment.getDoctor().getName();
        }

        String title = "Appointment " + appointment.getStatus().name();
        String message = cancellerName + " has "
                + appointment.getStatus().name().toLowerCase() + " the appointment on "
                + appointment.getScheduledDate() + " at " + appointment.getScheduledTime();

        if (appointment.getCancellationReason() != null) {
            message += ". Reason: " + appointment.getCancellationReason();
        }

        log.info("AppointmentCancelledEvent: {} by {}", appointment.getId(), cancelledBy);

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType("APPOINTMENT_CANCELLED");
        notification.setRecipientId(String.valueOf(recipientUser.getId()));
        notification.setReferenceId(appointment.getId().toString());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        sseEmitterService.sendToUser(recipientUser.getId(), "notification", Map.of(
                "type", "APPOINTMENT_CANCELLED",
                "title", title,
                "message", message));
    }
}