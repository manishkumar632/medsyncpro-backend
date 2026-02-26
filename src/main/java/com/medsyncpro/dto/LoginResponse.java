package com.medsyncpro.dto;

import com.medsyncpro.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String userId;
    private String email;
    private String name;
    private Role role;
    private com.medsyncpro.entity.VerificationStatus professionalVerificationStatus;
}
