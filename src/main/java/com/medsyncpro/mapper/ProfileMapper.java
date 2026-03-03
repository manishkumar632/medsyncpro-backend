package com.medsyncpro.mapper;

import com.medsyncpro.dto.response.DocumentResponse;
import com.medsyncpro.dto.response.ProfileResponse;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.medsyncpro.utils.UserProfileHelper;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProfileMapper {

    @Autowired
    private UserProfileHelper userProfileHelper;

    public ProfileResponse toProfileResponse(User user, List<Document> documents) {
        return ProfileResponse.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .name(userProfileHelper.getName(user))
                .phone(user.getPhone())
                .dob(userProfileHelper.getDob(user))
                .address(userProfileHelper.getAddress(user))
                .gender(userProfileHelper.getGender(user))
                .profileImageUrl(userProfileHelper.getProfileImage(user))
                .city(userProfileHelper.getCity(user))
                .state(userProfileHelper.getState(user))
                .bloodGroup(userProfileHelper.getBloodGroup(user))
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .updatedAt(user.getUpdatedAt())
                .bio(userProfileHelper.getBio(user))
                .experienceYears(userProfileHelper.getExperienceYears(user))
                .documents(documents.stream()
                        .map(this::toDocumentResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .documentTypeId(document.getDocumentType().getId())
                .typeName(document.getDocumentType().getName())
                .typeCode(document.getDocumentType().getCode())
                .url(document.getUrl())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
