package com.medsyncpro.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class DoctorDocumentUploadRequest {
    private UUID documentTypeId;
    private String publicId;
    private String secureUrl;
    private String resourceType;
    private String format;
    private String originalFilename;
    private Long bytes;
}
