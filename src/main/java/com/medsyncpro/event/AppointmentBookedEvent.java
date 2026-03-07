package com.medsyncpro.event;

import com.medsyncpro.entity.Appointment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppointmentBookedEvent extends ApplicationEvent {
    private final Appointment appointment;

    public AppointmentBookedEvent(Object source, Appointment appointment) {
        super(source);
        this.appointment = appointment;
    }
}
