package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules", indexes = {
        @Index(name = "idx_med_schedule_patient", columnList = "patient_id"),
        @Index(name = "idx_med_schedule_doctor", columnList = "doctor_id"),
        @Index(name = "idx_med_schedule_active", columnList = "active"),
        @Index(name = "idx_med_schedule_dates", columnList = "startDate,endDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE medication_schedules SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class MedicationSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(nullable = false)
    private String medicineName;

    private String dosage;

    private String frequency;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReminderScheduleType scheduleType = ReminderScheduleType.DAILY;

    @Column(columnDefinition = "TEXT")
    private String reminderTimes; // JSON array ["08:00","20:00"]

    @Column(columnDefinition = "TEXT")
    private String reminderDays; // JSON array ["MONDAY","WEDNESDAY"] for CUSTOM

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reminderInApp = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reminderEmail = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reminderPush = false;

    @Builder.Default
    @Column(nullable = false)
    private Double adherenceAlertThreshold = 80.0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
