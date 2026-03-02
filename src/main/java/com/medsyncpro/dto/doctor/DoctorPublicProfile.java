package com.medsyncpro.dto.doctor;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Full public profile of a doctor, returned when a user clicks on a doctor card.
 * Aggregates data from User, DoctorSettings, and DoctorClinic tables.
 * Sensitive fields (password, tokenVersion, etc.) are never included.
 */
@Data
@Builder
public class DoctorPublicProfile {

    // ── Basic Identity ──────────────────────────────────────────────
    private String id;
    private String name;
    private String email;            // Shown only if showContact privacy flag is true
    private String phone;            // Shown only if showContact privacy flag is true
    private String profileImageUrl;
    private String bio;
    private String gender;
    private String city;
    private String state;

    // ── Verification ────────────────────────────────────────────────
    private String verificationStatus;

    // ── Professional Info ───────────────────────────────────────────
    private String specialty;
    private String qualifications;
    private Integer experienceYears;
    private String medRegNumber;     // Shown publicly for transparency
    private Double consultationFee;
    private List<String> languages;
    private List<String> expertise;

    // ── Clinics ─────────────────────────────────────────────────────
    private List<ClinicResponse> clinics;

    // ── Availability ────────────────────────────────────────────────
    private Boolean availableForConsultation;
    private Boolean onlineConsultationEnabled;
    private Object weeklySchedule;   // Raw JSON schedule object — frontend renders it

    // ── Consultation Settings (public subset) ───────────────────────
    private Integer slotDurationMinutes;
    private Boolean autoApproveAppointments;
}