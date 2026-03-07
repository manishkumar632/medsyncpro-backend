package com.medsyncpro.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.medsyncpro.entity.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private UUID id;
    private UUID documentTypeId;
    private String typeName;
    private String type;
    private String typeCode;
    private String url;
    private String fileName;
    private Long fileSize;
    private String status;
    private LocalDateTime createdAt;

    public static DocumentResponse from(Document doc) {
        return DocumentResponse.builder()
                .type(doc.getDocumentType() != null ? doc.getDocumentType().getCode() : null)
                .url(doc.getFileUrl())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .build();
    }
}
