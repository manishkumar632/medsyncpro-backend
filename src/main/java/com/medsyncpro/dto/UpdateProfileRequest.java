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
    
    private String city;
    
    private String state;
    
    @Size(max = 10, message = "Blood group cannot exceed 10 characters")
    private String bloodGroup;
    
    private Boolean removeProfileImage;

    @Size(max = 2000, message = "Bio cannot exceed 2000 characters")
    private String bio;
}
