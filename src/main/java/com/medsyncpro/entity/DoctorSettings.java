package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_settings", indexes = {
    @Index(name = "idx_ds_user_id", columnList = "userId", unique = true)
})
@Data
public class DoctorSettings {
    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @PrePersist
    public void generateId() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    @Column(nullable = false, unique = true, length = 36)
    private String userId;

    // ── Professional Info ──
    @Column(length = 100)
    private String specialty;

    @Column(length = 255)
    private String qualifications;

    @Column(length = 100)
    private String medRegNumber;

    private Double consultationFee;

    @Column(columnDefinition = "TEXT")
    private String languages; // JSON array: ["English","Hindi"]

    @Column(columnDefinition = "TEXT")
    private String expertise; // JSON array: ["Cardiac Surgery"]

    // ── Consultation Settings ──
    @Column(nullable = false)
    private Integer slotDurationMinutes = 30;

    @Column(nullable = false)
    private Integer followUpWindowDays = 7;

    @Column(length = 100)
    private String prescriptionTemplate = "Default Template";

    @Column(nullable = false)
    private Boolean autoApproveAppointments = true;

    @Column(nullable = false)
    private Boolean onlineConsultationEnabled = true;

    @Column(nullable = false)
    private Boolean availableForConsultation = true;

    // ── Notification Preferences (JSON) ──
    @Column(columnDefinition = "TEXT")
    private String notificationPrefs;

    // ── Privacy Settings (JSON) ──
    @Column(columnDefinition = "TEXT")
    private String privacySettings;

    // ── Availability Schedule (JSON) ──
    @Column(columnDefinition = "TEXT")
    private String weeklySchedule;

    // ── Security ──
    @Column(nullable = false)
    private Boolean twoFactorEnabled = false;

    // ── Timestamps ──
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
