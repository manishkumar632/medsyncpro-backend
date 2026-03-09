package com.medsyncpro.dto.response;

import com.medsyncpro.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private Role senderRole; // DOCTOR | PATIENT
    private String senderName;
    private String senderAvatar;
    private String content;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}