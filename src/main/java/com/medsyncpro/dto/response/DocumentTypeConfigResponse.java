package com.medsyncpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long documentTypeId;
    private String name;
    private String code;
    private String description;
    private boolean required;
    private boolean active;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
