package com.medsyncpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentItem {
    private Long documentTypeId;
    private String typeCode;
    private String label;
    private boolean required;
    private boolean uploaded;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
