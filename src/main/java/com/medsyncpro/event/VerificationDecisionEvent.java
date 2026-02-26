package com.medsyncpro.event;

import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VerificationDecisionEvent extends ApplicationEvent {
    private final User user;
    private final VerificationStatus decision;
    private final String comments;

    public VerificationDecisionEvent(Object source, User user, VerificationStatus decision, String comments) {
        super(source);
        this.user = user;
        this.decision = decision;
        this.comments = comments;
    }
}
