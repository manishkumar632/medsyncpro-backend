package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private UUID id;

    // ── Doctor ────────────────────────────────────────────────────────────────
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorAvatar;

    // ── Patient ───────────────────────────────────────────────────────────────
    private UUID patientId;
    private String patientName;
    private String patientAvatar;

    // ── Preview ───────────────────────────────────────────────────────────────
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    /**
     * Unread count from the CALLER'S perspective.
     * MessagingService sets this based on who is requesting.
     */
    private int unreadCount;
}