package com.medsyncpro.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentItem {
    private UUID documentTypeId;
    private String typeCode;
    private String label;
    private String description;
    private boolean required;
    private boolean uploaded;
    private String status;       // NOT_UPLOADED | UPLOADED | PENDING | VERIFIED | REJECTED
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
