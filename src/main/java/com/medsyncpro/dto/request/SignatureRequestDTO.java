package com.medsyncpro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body sent by the client when requesting a Cloudinary upload signature.
 * documentTypeId identifies which document slot is being uploaded so the
 * server can validate it exists and is active for the caller's role.
 */
@Data
public class SignatureRequestDTO {

    @NotBlank(message = "documentTypeId is required")
    private String documentTypeId;
}