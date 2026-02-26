package com.medsyncpro.event;

import org.springframework.context.ApplicationEvent;
import com.medsyncpro.entity.User;
import lombok.Getter;

@Getter
public class UserSignupEvent extends ApplicationEvent {
    
    private final User user;

    public UserSignupEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
