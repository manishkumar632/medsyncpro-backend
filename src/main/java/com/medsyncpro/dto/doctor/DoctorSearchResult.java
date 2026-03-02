package com.medsyncpro.dto.doctor;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Lightweight DTO returned for each doctor in a search result list.
 * Contains just enough info to render a doctor card on the frontend.
 */
@Data
@Builder
public class DoctorSearchResult {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String bio;

    // Professional info (from DoctorSettings)
    private String specialty;
    private String qualifications;
    private Integer experienceYears;
    private Double consultationFee;
    private List<String> languages;
    private List<String> expertise;

    // Verification
    private String verificationStatus; // UNVERIFIED | VERIFIED | UNDER_REVIEW | etc.

    // Location
    private String city;
    private String state;

    // Primary clinic (if any)
    private String primaryClinicName;
    private String primaryClinicCity;

    // Availability flag
    private Boolean availableForConsultation;
    private Boolean onlineConsultationEnabled;
}