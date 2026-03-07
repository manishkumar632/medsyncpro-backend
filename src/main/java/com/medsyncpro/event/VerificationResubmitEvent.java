package com.medsyncpro.event;

import com.medsyncpro.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class VerificationResubmitEvent extends ApplicationEvent {

    private final User doctor;
    private final String comment;
    private final List<String> documentTypeCodes;

    public VerificationResubmitEvent(Object source,
            User doctor,
            String comment,
            List<String> documentTypeCodes) {
        super(source);
        this.doctor = doctor;
        this.comment = comment;
        this.documentTypeCodes = documentTypeCodes != null ? documentTypeCodes : List.of();
    }
}