package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_conv", columnList = "conversation_id"),
        @Index(name = "idx_chat_sender", columnList = "sender_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE chat_messages SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Denormalised role — avoids joining to doctors/patients tables
     * in bulk-read queries. Values: DOCTOR | PATIENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role senderRole;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    /** Timestamp set when the recipient opens the conversation. */
    private LocalDateTime readAt;
}