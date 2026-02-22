package com.medsyncpro.mapper;

import com.medsyncpro.dto.DocumentResponse;
import com.medsyncpro.dto.ProfileResponse;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProfileMapper {
    
    public ProfileResponse toProfileResponse(User user, List<Document> documents) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .dob(user.getDob())
                .address(user.getAddress())
                .gender(user.getGender())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .approved(user.getApproved())
                .emailVerified(user.getEmailVerified())
                .updatedAt(user.getUpdatedAt())
                .documents(documents.stream()
                        .map(this::toDocumentResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    public DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .type(document.getType())
                .url(document.getUrl())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
