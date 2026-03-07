package com.medsyncpro.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorProfileResponseDTO {
    private UUID doctorId;

    private String name;

    private String email;

    @Builder.Default
    private String phone = "";

    private boolean emailVerified;

    @Builder.Default
    private boolean phoneVerified = false;

    @Builder.Default
    private String licenseNumber = "";

    @Builder.Default
    private int experienceYears = 0;

    @Builder.Default
    private String qualification = "";

    @Builder.Default
    private String clinicName = "";

    @Builder.Default
    private String clinicAddress = "";

    @Builder.Default
    private String profileImage = "";

    @Builder.Default
    private String bio = "";

    @Builder.Default
    private double consultationFee = 0.0;

    @Builder.Default
    private boolean isVerified = false;

    private UUID specializationId;

    @Builder.Default
    private String specializationName = "";

    @Builder.Default
    private double averageRating = 0.0;

    @Builder.Default
    private int totalReviews = 0;

    @Builder.Default
    private int totalPatients = 0;

    @Builder.Default
    private String dob = "";

    @Builder.Default
    private String gender = "";

    @Builder.Default
    private String city = "";

    @Builder.Default
    private String state = "";

    @Builder.Default
    private String bloodGroup = "";

    @Builder.Default
    private String languageSpoken = "";

    @Builder.Default
    private String areaOfExperties = "";

    @Builder.Default
    private int consultationDuration = 30;

    @Builder.Default
    private int followUpWindow = 7;

}
