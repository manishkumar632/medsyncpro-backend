package com.medsyncpro.service;

import com.medsyncpro.dto.ProfileResponse;
import com.medsyncpro.dto.UpdateProfileRequest;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.mapper.ProfileMapper;
import com.medsyncpro.repository.DocumentRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ProfileMapper profileMapper;
    private final tools.jackson.databind.ObjectMapper objectMapper;
    
    @Transactional
    public ProfileResponse updateProfile(
            Long userId,
            String profileJson,
            MultipartFile profileImage,
            List<MultipartFile> documents,
            Map<String, String> documentTypes) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        List<String> uploadedUrls = new ArrayList<>();
        
        try {
            UpdateProfileRequest request = parseProfileRequest(profileJson);
            
            applyPartialUpdate(user, request);
            
            if (profileImage != null && !profileImage.isEmpty()) {
                String oldImageUrl = user.getProfileImageUrl();
                String newImageUrl = fileStorageService.uploadProfileImage(profileImage, userId);
                user.setProfileImageUrl(newImageUrl);
                uploadedUrls.add(newImageUrl);
                
                if (oldImageUrl != null) {
                    fileStorageService.deleteFile(oldImageUrl);
                }
            }
            
            if (request != null && Boolean.TRUE.equals(request.getRemoveProfileImage())) {
                String oldImageUrl = user.getProfileImageUrl();
                user.setProfileImageUrl(null);
                if (oldImageUrl != null) {
                    fileStorageService.deleteFile(oldImageUrl);
                }
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            
            List<Document> userDocuments = documentRepository.findByUserId(userId);
            
            if (documents != null && !documents.isEmpty()) {
                userDocuments = handleDocumentUploads(user, documents, documentTypes, uploadedUrls);
            }
            
            log.info("Profile updated successfully for user: {}", userId);
            return profileMapper.toProfileResponse(user, userDocuments);
            
        } catch (Exception e) {
            rollbackFileUploads(uploadedUrls);
            throw e;
        }
    }
    
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        List<Document> documents = documentRepository.findByUserId(userId);
        return profileMapper.toProfileResponse(user, documents);
    }
    
    private UpdateProfileRequest parseProfileRequest(String profileJson) {
        if (profileJson == null || profileJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(profileJson, UpdateProfileRequest.class);
        } catch (Exception e) {
            throw new BusinessException("INVALID_JSON", "Invalid profile data format");
        }
    }
    
    private void applyPartialUpdate(User user, UpdateProfileRequest request) {
        if (request == null) {
            return;
        }
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }
        
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }
        
        if (request.getDob() != null && !request.getDob().trim().isEmpty()) {
            try {
                user.setDob(LocalDate.parse(request.getDob()));
            } catch (DateTimeParseException e) {
                throw new BusinessException("INVALID_DATE", "Invalid date format. Use yyyy-MM-dd");
            }
        }
        
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
        }
        
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
    }
    
    private List<Document> handleDocumentUploads(
            User user,
            List<MultipartFile> files,
            Map<String, String> documentTypes,
            List<String> uploadedUrls) {
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
                continue;
            }
            
            String typeStr = documentTypes != null ? documentTypes.get("type_" + i) : null;
            DocumentType type = parseDocumentType(typeStr);
            
            String url = fileStorageService.uploadDocument(file, user.getId());
            uploadedUrls.add(url);
            
            Document document = new Document();
            document.setUserId(user.getId());
            document.setType(type);
            document.setUrl(url);
            document.setFileName(file.getOriginalFilename());
            document.setFileSize(file.getSize());
            
            documentRepository.save(document);
        }
        
        return documentRepository.findByUserId(user.getId());
    }
    
    private DocumentType parseDocumentType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return DocumentType.OTHER;
        }
        
        try {
            return DocumentType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DocumentType.OTHER;
        }
    }
    
    private void rollbackFileUploads(List<String> uploadedUrls) {
        for (String url : uploadedUrls) {
            try {
                fileStorageService.deleteFile(url);
            } catch (Exception e) {
                log.error("Failed to rollback file: {}", url, e);
            }
        }
    }
}
