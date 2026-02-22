package com.medsyncpro.dto;

import com.medsyncpro.entity.DocumentType;
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
    private DocumentType type;
    private String url;
    private String fileName;
    private Long fileSize;
    private LocalDateTime createdAt;
}
