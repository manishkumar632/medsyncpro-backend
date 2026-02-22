package com.medsyncpro.dto;

import com.medsyncpro.entity.Gender;
import com.medsyncpro.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private LocalDate dob;
    private String address;
    private Gender gender;
    private String profileImageUrl;
    private Role role;
    private Boolean approved;
    private Boolean emailVerified;
    private LocalDateTime updatedAt;
    private List<DocumentResponse> documents;
}
