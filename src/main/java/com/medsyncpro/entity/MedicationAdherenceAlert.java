package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_adherence_alerts", indexes = {
        @Index(name = "idx_med_alert_patient", columnList = "patient_id"),
        @Index(name = "idx_med_alert_doctor", columnList = "doctor_id"),
        @Index(name = "idx_med_alert_alerted_at", columnList = "alertedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE medication_adherence_alerts SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class MedicationAdherenceAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private Double adherencePercentage;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private LocalDateTime alertedAt;

    @PrePersist
    public void onCreate() {
        if (alertedAt == null) {
            alertedAt = LocalDateTime.now();
        }
    }
}
