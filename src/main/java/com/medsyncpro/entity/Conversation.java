package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_conv_doctor_patient", columnNames = { "doctor_id", "patient_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE conversations SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Truncated preview of the last message — avoids a JOIN on list queries. */
    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    /** How many unread messages the DOCTOR still has in this conversation. */
    @Builder.Default
    @Column(nullable = false)
    private int doctorUnreadCount = 0;

    /** How many unread messages the PATIENT still has in this conversation. */
    @Builder.Default
    @Column(nullable = false)
    private int patientUnreadCount = 0;
}