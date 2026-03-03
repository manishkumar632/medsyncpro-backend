package com.medsyncpro.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeRequest {
    private String name;
    private String description;
    /** One of: DOCTOR, PHARMACIST, AGENT */
    private String modelType;
    private boolean required = true;
    private Integer displayOrder;
}
