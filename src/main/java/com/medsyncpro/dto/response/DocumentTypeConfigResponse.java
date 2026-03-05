package com.medsyncpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin-facing response for document type configuration per model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeConfigResponse {
    /** ModelDocumentType mapping ID */
    private Long id;
    /** DocumentTypeEntity ID */
    private UUID documentTypeId;
    /** The user model this config is assigned to */
    private String modelType;
    private String name;
    private String code;
    private String description;
    private boolean required;
    private boolean active;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
