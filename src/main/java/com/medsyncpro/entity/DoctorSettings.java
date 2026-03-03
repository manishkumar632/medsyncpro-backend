package com.medsyncpro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "doctor_settings", indexes = {
    @Index(name = "idx_ds_user_id", columnList = "userId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE doctor_settings SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class DoctorSettings extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

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
    @Builder.Default
    @Column(nullable = false)
    private Integer slotDurationMinutes = 30;

    @Builder.Default
    @Column(nullable = false)
    private Integer followUpWindowDays = 7;

    @Builder.Default
    @Column(length = 100)
    private String prescriptionTemplate = "Default Template";

    @Builder.Default
    @Column(nullable = false)
    private Boolean autoApproveAppointments = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean onlineConsultationEnabled = true;

    @Builder.Default
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
    @Builder.Default
    @Column(nullable = false)
    private Boolean twoFactorEnabled = false;
}
