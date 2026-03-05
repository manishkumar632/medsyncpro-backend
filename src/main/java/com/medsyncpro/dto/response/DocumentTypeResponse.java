package com.medsyncpro.dto.response;

import java.util.UUID;

import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentTypeResponse {

    private UUID id;
    private Role role;
    private String name;
    private String code;
    private boolean required;
    private boolean active;
    private int displayOrder;
    private String description;

    public static DocumentTypeResponse from(DocumentType docs) {
        return DocumentTypeResponse.builder()
                .id(docs.getId())
                .role(docs.getRole())
                .name(docs.getName())
                .code(docs.getCode())
                .required(docs.isRequired())
                .active(docs.isActive())
                .displayOrder(docs.getDisplayOrder())
                .description(docs.getDescription())
                .build();
    }
}