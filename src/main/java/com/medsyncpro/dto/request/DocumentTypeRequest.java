package com.medsyncpro.dto.request;

import com.medsyncpro.entity.Role;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeRequest {

    @NotBlank(message = "Document type name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Role type is required")
    private Role role;

    @NotBlank(message = "Document type Code is required")
    private String code;

    private boolean required = false;

    private boolean active = true;
    
    @Min(value = 1, message = "Display order cannot be negative")
    private int displayOrder;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
}
