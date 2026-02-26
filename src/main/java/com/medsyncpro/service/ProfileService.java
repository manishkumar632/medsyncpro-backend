package com.medsyncpro.service;

import com.medsyncpro.dto.ProfileResponse;
import com.medsyncpro.dto.RequiredDocumentItem;
import com.medsyncpro.dto.UpdateProfileRequest;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.entity.VerificationStatus;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.mapper.ProfileMapper;
import com.medsyncpro.repository.DocumentRepository;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileMapper profileMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    // ── Required documents configuration ──
    
    private static final List<RequiredDocInfo> REQUIRED_DOCS = List.of(
            new RequiredDocInfo(DocumentType.LICENSE, "Medical License / Medical Certificate", true),
            new RequiredDocInfo(DocumentType.ID_PROOF, "Government ID Proof", true),
            new RequiredDocInfo(DocumentType.DEGREE, "Degree Certificate", true)
    );

    private record RequiredDocInfo(DocumentType type, String label, boolean required) {}

    // ── Required documents checklist ──
    
    @Transactional(readOnly = true)
    public List<RequiredDocumentItem> getRequiredDocuments(String userId) {
        List<Document> userDocs = documentRepository.findByUserId(userId);
        Map<DocumentType, Document> docMap = new HashMap<>();
        for (Document doc : userDocs) {
            docMap.put(doc.getType(), doc);
        }
        
        return REQUIRED_DOCS.stream()
                .map(info -> {
                    Document doc = docMap.get(info.type());
                    return RequiredDocumentItem.builder()
                            .type(info.type())
                            .label(info.label())
                            .required(info.required())
                            .uploaded(doc != null)
                            .fileUrl(doc != null ? doc.getUrl() : null)
                            .fileName(doc != null ? doc.getFileName() : null)
                            .fileSize(doc != null ? doc.getFileSize() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    // ── Single document upload by type ──
    
    @Transactional
    public com.medsyncpro.dto.VerificationStatusResponse uploadSingleDocument(String userId, DocumentType type, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        // Don't allow upload if already verified or under review
        VerificationStatus currentStatus = user.getProfessionalVerificationStatus();
        if (currentStatus == VerificationStatus.VERIFIED) {
            throw new BusinessException("ALREADY_VERIFIED", "Your account is already verified");
        }
        if (currentStatus == VerificationStatus.UNDER_REVIEW) {
            throw new BusinessException("UNDER_REVIEW", "Your verification is under review. You cannot modify documents");
        }
        
        String url = fileStorageService.uploadDocument(file, userId);
        
        try {
            // Check if document of this type already exists — replace it
            Optional<Document> existing = documentRepository.findByUserIdAndType(userId, type);
            if (existing.isPresent()) {
                Document existingDoc = existing.get();
                // Delete old file
                try {
                    fileStorageService.deleteFile(existingDoc.getUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete old document file: {}", existingDoc.getUrl());
                }
                existingDoc.setUrl(url);
                existingDoc.setFileName(file.getOriginalFilename());
                existingDoc.setFileSize(file.getSize());
                existingDoc.setStatus("UPLOADED");
                existingDoc.setCreatedAt(LocalDateTime.now());
                documentRepository.save(existingDoc);
            } else {
                Document document = new Document();
                document.setUserId(userId);
                document.setType(type);
                document.setUrl(url);
                document.setFileName(file.getOriginalFilename());
                document.setFileSize(file.getSize());
                document.setStatus("UPLOADED");
                documentRepository.save(document);
            }
            
            // Update user status to DOCUMENT_SUBMITTED if UNVERIFIED or REJECTED
            if (currentStatus == VerificationStatus.UNVERIFIED || currentStatus == VerificationStatus.REJECTED) {
                user.setProfessionalVerificationStatus(VerificationStatus.DOCUMENT_SUBMITTED);
                userRepository.save(user);
            }
            
            return getVerificationStatus(userId);
            
        } catch (Exception e) {
            // Rollback file upload on error
            try {
                fileStorageService.deleteFile(url);
            } catch (Exception ex) {
                log.error("Failed to rollback uploaded file: {}", url);
            }
            throw e;
        }
    }
    
    // ── Delete a single document by type ──
    
    @Transactional
    public com.medsyncpro.dto.VerificationStatusResponse deleteSingleDocument(String userId, DocumentType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VerificationStatus currentStatus = user.getProfessionalVerificationStatus();
        if (currentStatus == VerificationStatus.VERIFIED) {
            throw new BusinessException("ALREADY_VERIFIED", "Cannot modify documents — already verified");
        }
        if (currentStatus == VerificationStatus.UNDER_REVIEW) {
            throw new BusinessException("UNDER_REVIEW", "Cannot modify documents while under review");
        }
        
        Optional<Document> existing = documentRepository.findByUserIdAndType(userId, type);
        if (existing.isPresent()) {
            try {
                fileStorageService.deleteFile(existing.get().getUrl());
            } catch (Exception e) {
                log.warn("Failed to delete file: {}", existing.get().getUrl());
            }
            documentRepository.deleteByUserIdAndType(userId, type);
        }
        
        return getVerificationStatus(userId);
    }
    
    // ── Submit for verification ──
    
    @Transactional
    public com.medsyncpro.dto.VerificationStatusResponse submitForVerification(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VerificationStatus currentStatus = user.getProfessionalVerificationStatus();
        if (currentStatus == VerificationStatus.VERIFIED) {
            throw new BusinessException("ALREADY_VERIFIED", "Your account is already verified");
        }
        if (currentStatus == VerificationStatus.UNDER_REVIEW) {
            throw new BusinessException("ALREADY_SUBMITTED", "Your verification is already under review");
        }
        
        // Validate all required documents are uploaded
        List<Document> userDocs = documentRepository.findByUserId(userId);
        Set<DocumentType> uploadedTypes = userDocs.stream()
                .map(Document::getType)
                .collect(Collectors.toSet());
        
        List<String> missingDocs = REQUIRED_DOCS.stream()
                .filter(RequiredDocInfo::required)
                .filter(info -> !uploadedTypes.contains(info.type()))
                .map(RequiredDocInfo::label)
                .collect(Collectors.toList());
        
        if (!missingDocs.isEmpty()) {
            throw new BusinessException("MISSING_DOCUMENTS", 
                    "Missing required documents: " + String.join(", ", missingDocs));
        }
        
        // Update status to UNDER_REVIEW
        user.setProfessionalVerificationStatus(VerificationStatus.UNDER_REVIEW);
        userRepository.save(user);
        
        // Update or create verification request
        VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
        if (request == null) {
            request = new VerificationRequest();
            request.setUser(user);
        }
        request.setStatus(VerificationStatus.UNDER_REVIEW);
        request.setSubmittedAt(LocalDateTime.now());
        request.setReviewNotes(null);
        request.setReviewedAt(null);
        verificationRequestRepository.save(request);
        
        // Notify admins
        eventPublisher.publishEvent(new com.medsyncpro.event.DocumentSubmittedEvent(this, user));
        
        log.info("User {} submitted for verification", userId);
        return getVerificationStatus(userId);
    }
    
    // ── Existing methods ──

    @Transactional
    public ProfileResponse updateProfile(
            String userId,
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
    public ProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        List<Document> documents = documentRepository.findByUserId(userId);
        return profileMapper.toProfileResponse(user, documents);
    }
    
    /**
     * Simple JSON-based profile update (no file uploads).
     */
    @Transactional
    public ProfileResponse simpleUpdateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        applyPartialUpdate(user, request);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        
        List<Document> documents = documentRepository.findByUserId(userId);
        log.info("Profile updated (JSON) for user: {}", userId);
        return profileMapper.toProfileResponse(user, documents);
    }
    
    @Transactional(readOnly = true)
    public com.medsyncpro.dto.VerificationStatusResponse getVerificationStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
        String reviewNotes = request != null ? request.getReviewNotes() : null;
        LocalDateTime submittedAt = request != null ? request.getSubmittedAt() : null;
        LocalDateTime reviewedAt = request != null ? request.getReviewedAt() : null;
        
        List<Document> documents = documentRepository.findByUserId(userId);
        List<com.medsyncpro.dto.DocumentResponse> documentResponses = documents.stream()
                .map(profileMapper::toDocumentResponse)
                .collect(Collectors.toList());
        
        List<RequiredDocumentItem> requiredDocs = getRequiredDocuments(userId);
                
        return com.medsyncpro.dto.VerificationStatusResponse.builder()
                .status(user.getProfessionalVerificationStatus())
                .submittedDocuments(documentResponses)
                .requiredDocuments(requiredDocs)
                .verificationNotes(reviewNotes)
                .submittedAt(submittedAt)
                .reviewedAt(reviewedAt)
                .build();
    }
    
    @Transactional
    public com.medsyncpro.dto.VerificationStatusResponse uploadVerificationDocuments(
            String userId, 
            List<MultipartFile> documents, 
            Map<String, String> documentTypes) {
            
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        List<String> uploadedUrls = new ArrayList<>();
        
        try {
            if (documents != null && !documents.isEmpty()) {
                handleDocumentUploads(user, documents, documentTypes, uploadedUrls);
            }
            
            user.setProfessionalVerificationStatus(VerificationStatus.DOCUMENT_SUBMITTED);
            user = userRepository.save(user);
            
            VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
            if (request == null) {
                request = new VerificationRequest();
                request.setUser(user);
            }
            request.setStatus(VerificationStatus.DOCUMENT_SUBMITTED);
            verificationRequestRepository.save(request);
            
            // Publish Event to notify admins
            eventPublisher.publishEvent(new com.medsyncpro.event.DocumentSubmittedEvent(this, user));
            
        } catch (Exception e) {
            rollbackFileUploads(uploadedUrls);
            throw e;
        }
        
        return getVerificationStatus(userId);
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
        
        if (request.getCity() != null) {
            user.setCity(request.getCity().trim().isEmpty() ? null : request.getCity().trim());
        }
        
        if (request.getState() != null) {
            user.setState(request.getState().trim().isEmpty() ? null : request.getState().trim());
        }
        
        if (request.getBloodGroup() != null) {
            user.setBloodGroup(request.getBloodGroup().trim().isEmpty() ? null : request.getBloodGroup().trim());
        }
        
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim().isEmpty() ? null : request.getBio().trim());
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
