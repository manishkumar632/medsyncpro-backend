package com.medsyncpro.dto;

import com.medsyncpro.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "invalid email")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$", 
            message = "Password must be at least 8 characters with uppercase, lowercase, number and special character")
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    @AssertTrue(message = "Please accept terms and condition")
    private Boolean termsAccepted;
}
