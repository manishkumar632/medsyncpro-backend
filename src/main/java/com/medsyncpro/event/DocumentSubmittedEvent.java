package com.medsyncpro.event;

import com.medsyncpro.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentSubmittedEvent extends ApplicationEvent {
    private final User user;

    public DocumentSubmittedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
