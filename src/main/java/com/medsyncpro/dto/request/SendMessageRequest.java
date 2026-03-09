package com.medsyncpro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 5000, message = "Message must not exceed 5 000 characters")
    private String content;
}