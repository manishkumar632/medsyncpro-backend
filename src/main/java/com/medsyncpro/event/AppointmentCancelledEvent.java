package com.medsyncpro.event;

import com.medsyncpro.entity.Appointment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentCancelledEvent extends ApplicationEvent {
    private final Appointment appointment;
    private final String cancelledBy; // "PATIENT" or "DOCTOR"

    public AppointmentCancelledEvent(Object source, Appointment appointment, String cancelledBy) {
        super(source);
        this.appointment = appointment;
        this.cancelledBy = cancelledBy;
    }
}
