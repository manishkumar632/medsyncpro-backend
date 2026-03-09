package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_tracker_entries", indexes = {
        @Index(name = "idx_health_tracker_patient", columnList = "patient_id"),
        @Index(name = "idx_health_tracker_metric", columnList = "metricType"),
        @Index(name = "idx_health_tracker_recorded_at", columnList = "recordedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE health_tracker_entries SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class HealthTrackerEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HealthMetricType metricType;

    @Column(nullable = false)
    private String metricValue;

    private String unit;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
