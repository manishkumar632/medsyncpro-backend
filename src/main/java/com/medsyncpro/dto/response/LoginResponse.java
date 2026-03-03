package com.medsyncpro.dto.response;

import com.medsyncpro.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String email;
    private Role role;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String phone;
    private boolean termsAccepted;
}
