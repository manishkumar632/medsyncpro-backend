package com.medsyncpro.event;

import com.medsyncpro.entity.Appointment;
import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.Role;
import com.medsyncpro.repository.NotificationRepository;
import com.medsyncpro.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    @Async
    @EventListener
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        Appointment appointment = event.getAppointment();

        // Notify Doctor
        String doctorMessage = String.format("New appointment booked by %s for %s",
                appointment.getPatient().getName(),
                appointment.getScheduledDate().toString());

        Notification doctorNotification = new Notification();
        doctorNotification.setRecipientId(appointment.getDoctor().getUser().getId().toString());
        doctorNotification.setTitle("New Appointment");
        doctorNotification.setMessage(doctorMessage);
        doctorNotification.setType("APPOINTMENT");
        doctorNotification.setReferenceId(appointment.getId().toString());

        notificationRepository.save(doctorNotification);

        sseEmitterService.sendToUser(appointment.getDoctor().getUser().getId(), "notification",
                Map.of("title", "New Appointment", "message", doctorMessage));

        log.info("Notified doctor {} about new appointment {}", appointment.getDoctor().getId(), appointment.getId());
    }

    @Async
    @EventListener
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        Appointment appointment = event.getAppointment();
        String cancelledBy = event.getCancelledBy();

        // If patient cancelled, notify doctor
        if ("PATIENT".equals(cancelledBy)) {
            String message = String.format("Appointment on %s was cancelled by patient %s",
                    appointment.getScheduledDate().toString(),
                    appointment.getPatient().getName());

            Notification notification = new Notification();
            notification.setRecipientId(appointment.getDoctor().getUser().getId().toString());
            notification.setTitle("Appointment Cancelled");
            notification.setMessage(message);
            notification.setType("APPOINTMENT");
            notification.setReferenceId(appointment.getId().toString());

            notificationRepository.save(notification);

            sseEmitterService.sendToUser(appointment.getDoctor().getUser().getId(), "notification",
                    Map.of("title", "Appointment Cancelled", "message", message));

            log.info("Notified doctor {} about cancelled appointment {}", appointment.getDoctor().getId(),
                    appointment.getId());
        }
        // If doctor cancelled, notify patient
        else if ("DOCTOR".equals(cancelledBy)) {
            String message = String.format("Your appointment on %s was cancelled by Dr. %s",
                    appointment.getScheduledDate().toString(),
                    appointment.getDoctor().getName());

            Notification notification = new Notification();
            notification.setRecipientId(appointment.getPatient().getUser().getId().toString());
            notification.setTitle("Appointment Cancelled");
            notification.setMessage(message);
            notification.setType("APPOINTMENT");
            notification.setReferenceId(appointment.getId().toString());

            notificationRepository.save(notification);

            sseEmitterService.sendToUser(appointment.getPatient().getUser().getId(), "notification",
                    Map.of("title", "Appointment Cancelled", "message", message));

            log.info("Notified patient {} about cancelled appointment {}", appointment.getPatient().getId(),
                    appointment.getId());
        }
    }
}
