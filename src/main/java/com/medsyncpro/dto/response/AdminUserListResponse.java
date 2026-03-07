package com.medsyncpro.dto.response;

import com.medsyncpro.entity.Gender;
import com.medsyncpro.entity.Role;
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
public class AdminUserListResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private Boolean emailVerified;
    private Boolean deleted;
    private String profileImageUrl;
    private Gender gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentResponse> documents;
    private String professionalVerificationStatus;
}