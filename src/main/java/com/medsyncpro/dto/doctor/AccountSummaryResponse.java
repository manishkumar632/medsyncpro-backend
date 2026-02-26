package com.medsyncpro.dto.doctor;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountSummaryResponse {
    private String userId;
    private String email;
    private String name;
    private String role;
    private String verificationStatus;
    private Boolean emailVerified;
    private Boolean isActive;
    private LocalDateTime memberSince;
    private LocalDateTime lastUpdated;
}
