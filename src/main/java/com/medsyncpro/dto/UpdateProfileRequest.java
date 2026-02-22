package com.medsyncpro.dto;

import com.medsyncpro.entity.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    
    private String dob; // Format: yyyy-MM-dd
    
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;
    
    private Gender gender;
    
    private Boolean removeProfileImage;
}
