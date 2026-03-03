package com.medsyncpro.dto.response;

import com.medsyncpro.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerificationDetailResponse {
    private String id;
    private VerificationStatus status;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    // User info
    private UserSummary user;

    // Documents belonging to this user
    private List<DocumentResponse> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String role;
        private String profileImageUrl;
        private LocalDateTime createdAt;
    }
}
