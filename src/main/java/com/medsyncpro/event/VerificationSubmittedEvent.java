package com.medsyncpro.event;

import com.medsyncpro.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VerificationSubmittedEvent extends ApplicationEvent {

    private final User submittedBy; // the doctor who submitted

    public VerificationSubmittedEvent(Object source, User submittedBy) {
        super(source);
        this.submittedBy = submittedBy;
    }
}