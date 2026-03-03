package com.medsyncpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long documentTypeId;
    private String typeName;
    private String typeCode;
    private String url;
    private String fileName;
    private Long fileSize;
    private LocalDateTime createdAt;
}
