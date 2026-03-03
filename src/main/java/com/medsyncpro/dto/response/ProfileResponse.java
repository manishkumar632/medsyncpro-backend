package com.medsyncpro.dto.response;

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
    private String id;
    private String email;
    private String name;
    private String phone;
    private LocalDate dob;
    private String address;
    private Gender gender;
    private String profileImageUrl;
    private String city;
    private String state;
    private String bloodGroup;
    private Role role;
    private Boolean emailVerified;
    private LocalDateTime updatedAt;
    private List<DocumentResponse> documents;
    private String bio;
    private Integer experienceYears;
}
