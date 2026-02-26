package com.medsyncpro.dto;

import com.medsyncpro.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentItem {
    private DocumentType type;
    private String label;
    private boolean required;
    private boolean uploaded;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
