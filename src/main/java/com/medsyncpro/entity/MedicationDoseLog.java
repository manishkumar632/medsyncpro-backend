package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_dose_logs", indexes = {
        @Index(name = "idx_med_dose_schedule", columnList = "schedule_id"),
        @Index(name = "idx_med_dose_patient", columnList = "patient_id"),
        @Index(name = "idx_med_dose_status", columnList = "status"),
        @Index(name = "idx_med_dose_scheduled_at", columnList = "scheduledAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE medication_dose_logs SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class MedicationDoseLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime takenAt;

    private LocalDateTime snoozedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DoseStatus status = DoseStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reminderSent = false;

    @Column(columnDefinition = "TEXT")
    private String note;
}
