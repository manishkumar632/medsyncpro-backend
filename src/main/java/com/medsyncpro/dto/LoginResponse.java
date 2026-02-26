package com.medsyncpro.dto;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.VerificationStatus;
import lombok.Data;

@Data
public class LoginResponse {
    private String userId;
    private String email;
    private String name;
    private Role role;
    private VerificationStatus professionalVerificationStatus;

    // Verification capability flags
    private boolean canPrescribe;
    private boolean canAcceptAppointments;
    private boolean canAccessFullDashboard;

    public LoginResponse(String userId, String email, String name, Role role, VerificationStatus status) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.professionalVerificationStatus = status;

        // Capabilities are granted only when VERIFIED
        boolean isVerified = status == VerificationStatus.VERIFIED;
        boolean isNonDoctor = role != Role.DOCTOR;
        this.canPrescribe = isVerified || isNonDoctor;
        this.canAcceptAppointments = isVerified || isNonDoctor;
        this.canAccessFullDashboard = isVerified || isNonDoctor;
    }
}
