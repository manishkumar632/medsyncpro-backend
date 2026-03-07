package com.medsyncpro.service;

import com.medsyncpro.dto.response.ProfileResponse;
import com.medsyncpro.dto.response.RequiredDocumentItem;
import com.medsyncpro.dto.request.UpdateProfileRequest;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.UserModelType;
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
import com.medsyncpro.utils.UserProfileHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileHelper userProfileHelper;
    private final DocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileMapper profileMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    // ── Required documents checklist (now dynamic from DB) ──

    // @Transactional(readOnly = true)
    // public List<RequiredDocumentItem> getRequiredDocuments(String userId) {
    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     UserModelType modelType = roleToModelType(user.getRole());
    //     if (modelType == null) {
    //         return Collections.emptyList();
    //     }

    //     List<ModelDocumentType> configs = modelDocumentTypeRepository
    //             .findByModelTypeAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(modelType);

    //     List<Document> userDocs = documentRepository.findByUserId(userId);
    //     Map<Long, Document> docMap = new HashMap<>();
    //     for (Document doc : userDocs) {
    //         docMap.put(doc.getDocumentType().getId(), doc);
    //     }

    //     return configs.stream()
    //             .map(config -> {
    //                 DocumentTypeEntity dt = config.getDocumentType();
    //                 Document doc = docMap.get(dt.getId());
    //                 return RequiredDocumentItem.builder()
    //                         .documentTypeId(dt.getId())
    //                         .typeCode(dt.getCode())
    //                         .label(dt.getName())
    //                         .required(config.isRequired())
    //                         .uploaded(doc != null)
    //                         .fileUrl(doc != null ? doc.getUrl() : null)
    //                         .fileName(doc != null ? doc.getFileName() : null)
    //                         .fileSize(doc != null ? doc.getFileSize() : null)
    //                         .build();
    //             })
    //             .collect(Collectors.toList());
    // }

    // ── Single document upload by document type ID ──

    // @Transactional
    // public com.medsyncpro.dto.response.VerificationStatusResponse uploadSingleDocument(
    //         String userId, Long documentTypeId, MultipartFile file) {

    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     if (user.getDeleted()) {
    //         throw new BusinessException("USER_DELETED", "User account is deleted");
    //     }

    //     DocumentTypeEntity docType = documentTypeEntityRepository.findByIdAndDeletedFalse(documentTypeId)
    //             .orElseThrow(() -> new BusinessException("INVALID_DOCUMENT_TYPE",
    //                     "Invalid document type ID: " + documentTypeId));

    //     // Don't allow upload if already verified or under review
    //     VerificationStatus currentStatus = userProfileHelper.getVerificationStatus(user);
    //     if (currentStatus == VerificationStatus.VERIFIED) {
    //         throw new BusinessException("ALREADY_VERIFIED", "Your account is already verified");
    //     }
    //     if (currentStatus == VerificationStatus.UNDER_REVIEW) {
    //         throw new BusinessException("UNDER_REVIEW",
    //                 "Your verification is under review. You cannot modify documents");
    //     }

    //     // Check that this doc type is active for user's model
    //     UserModelType modelType = roleToModelType(user.getRole());
    //     if (modelType != null) {
    //         boolean isAssigned = modelDocumentTypeRepository
    //                 .existsByModelTypeAndDocumentTypeAndDeletedFalse(modelType, docType);
    //         if (!isAssigned) {
    //             throw new BusinessException("DOCUMENT_TYPE_NOT_ASSIGNED",
    //                     "Document type '" + docType.getName() + "' is not configured for your role");
    //         }
    //     }

    //     String url = fileStorageService.uploadDocument(file, userId);

    //     try {
    //         // Check if document of this type already exists — replace it
    //         Optional<Document> existing = documentRepository.findByUserIdAndDocumentType(userId, docType);
    //         if (existing.isPresent()) {
    //             Document existingDoc = existing.get();
    //             try {
    //                 fileStorageService.deleteFile(existingDoc.getUrl());
    //             } catch (Exception e) {
    //                 log.warn("Failed to delete old document file: {}", existingDoc.getUrl());
    //             }
    //             existingDoc.setUrl(url);
    //             existingDoc.setFileName(file.getOriginalFilename());
    //             existingDoc.setFileSize(file.getSize());
    //             existingDoc.setStatus("UPLOADED");
    //             existingDoc.setCreatedAt(LocalDateTime.now());
    //             documentRepository.save(existingDoc);
    //         } else {
    //             Document document = new Document();
    //             document.setUserId(userId);
    //             document.setDocumentType(docType);
    //             document.setUrl(url);
    //             document.setFileName(file.getOriginalFilename());
    //             document.setFileSize(file.getSize());
    //             document.setStatus("UPLOADED");
    //             documentRepository.save(document);
    //         }

    //         // Update user status
    //         if (currentStatus == VerificationStatus.UNVERIFIED || currentStatus == VerificationStatus.REJECTED) {
    //             userRepository.save(user);
    //         }

    //         return getVerificationStatus(userId);

    //     } catch (Exception e) {
    //         try {
    //             fileStorageService.deleteFile(url);
    //         } catch (Exception ex) {
    //             log.error("Failed to rollback uploaded file: {}", url);
    //         }
    //         throw e;
    //     }
    // }

    // ── Delete a single document by document type ID ──

    // @Transactional
    // public com.medsyncpro.dto.response.VerificationStatusResponse deleteSingleDocument(
    //         String userId, Long documentTypeId) {

    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     DocumentTypeEntity docType = documentTypeEntityRepository.findByIdAndDeletedFalse(documentTypeId)
    //             .orElseThrow(() -> new BusinessException("INVALID_DOCUMENT_TYPE",
    //                     "Invalid document type ID: " + documentTypeId));

    //     VerificationStatus currentStatus = userProfileHelper.getVerificationStatus(user);
    //     if (currentStatus == VerificationStatus.VERIFIED) {
    //         throw new BusinessException("ALREADY_VERIFIED", "Cannot modify documents — already verified");
    //     }
    //     if (currentStatus == VerificationStatus.UNDER_REVIEW) {
    //         throw new BusinessException("UNDER_REVIEW", "Cannot modify documents while under review");
    //     }

    //     Optional<Document> existing = documentRepository.findByUserIdAndDocumentType(userId, docType);
    //     if (existing.isPresent()) {
    //         try {
    //             fileStorageService.deleteFile(existing.get().getUrl());
    //         } catch (Exception e) {
    //             log.warn("Failed to delete file: {}", existing.get().getUrl());
    //         }
    //         documentRepository.deleteByUserIdAndDocumentType(userId, docType);
    //     }

    //     return getVerificationStatus(userId);
    // }

    // ── Submit for verification ──

    // @Transactional
    // public com.medsyncpro.dto.response.VerificationStatusResponse submitForVerification(String userId) {
    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     VerificationStatus currentStatus = userProfileHelper.getVerificationStatus(user);
    //     if (currentStatus == VerificationStatus.VERIFIED) {
    //         throw new BusinessException("ALREADY_VERIFIED", "Your account is already verified");
    //     }
    //     if (currentStatus == VerificationStatus.UNDER_REVIEW) {
    //         throw new BusinessException("ALREADY_SUBMITTED", "Your verification is already under review");
    //     }

    //     // Validate all REQUIRED documents are uploaded (dynamic from DB)
    //     UserModelType modelType = roleToModelType(user.getRole());
    //     if (modelType != null) {
    //         List<ModelDocumentType> configs = modelDocumentTypeRepository
    //                 .findByModelTypeAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(modelType);

    //         List<Document> userDocs = documentRepository.findByUserId(userId);
    //         Set<Long> uploadedTypeIds = userDocs.stream()
    //                 .map(d -> d.getDocumentType().getId())
    //                 .collect(Collectors.toSet());

    //         List<String> missingDocs = configs.stream()
    //                 .filter(ModelDocumentType::isRequired)
    //                 .filter(c -> !uploadedTypeIds.contains(c.getDocumentType().getId()))
    //                 .map(c -> c.getDocumentType().getName())
    //                 .collect(Collectors.toList());

    //         if (!missingDocs.isEmpty()) {
    //             throw new BusinessException("MISSING_DOCUMENTS",
    //                     "Missing required documents: " + String.join(", ", missingDocs));
    //         }
    //     }

    //     userRepository.save(user);

    //     // Update or create verification request
    //     VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
    //     if (request == null) {
    //         request = new VerificationRequest();
    //         request.setUser(user);
    //     }
    //     request.setStatus(VerificationStatus.UNDER_REVIEW);
    //     request.setSubmittedAt(LocalDateTime.now());
    //     request.setReviewNotes(null);
    //     request.setReviewedAt(null);
    //     verificationRequestRepository.save(request);

    //     eventPublisher.publishEvent(new com.medsyncpro.event.DocumentSubmittedEvent(this, user));

    //     log.info("User {} submitted for verification", userId);
    //     return getVerificationStatus(userId);
    // }

    // ── Existing methods ──

    // @Transactional
    // public ProfileResponse updateProfile(
    //         String userId,
    //         String profileJson,
    //         MultipartFile profileImage,
    //         List<MultipartFile> documents,
    //         Map<String, String> documentTypes) {

    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     if (user.getDeleted()) {
    //         throw new BusinessException("USER_DELETED", "User account is deleted");
    //     }

    //     List<String> uploadedUrls = new ArrayList<>();

    //     try {
    //         UpdateProfileRequest request = parseProfileRequest(profileJson);

    //         applyPartialUpdate(user, request);

    //         if (profileImage != null && !profileImage.isEmpty()) {
    //             String oldImageUrl = userProfileHelper.getProfileImage(user);
    //             String newImageUrl = fileStorageService.uploadProfileImage(profileImage, userId);
    //             uploadedUrls.add(newImageUrl);

    //             if (oldImageUrl != null) {
    //                 fileStorageService.deleteFile(oldImageUrl);
    //             }
    //         }

    //         if (request != null && Boolean.TRUE.equals(request.getRemoveProfileImage())) {
    //             String oldImageUrl = userProfileHelper.getProfileImage(user);
    //             if (oldImageUrl != null) {
    //                 fileStorageService.deleteFile(oldImageUrl);
    //             }
    //         }

    //         user.setUpdatedAt(LocalDateTime.now());
    //         user = userRepository.save(user);

    //         List<Document> userDocuments = documentRepository.findByUserId(userId);

    //         if (documents != null && !documents.isEmpty()) {
    //             userDocuments = handleDocumentUploads(user, documents, documentTypes, uploadedUrls);
    //         }

    //         log.info("Profile updated successfully for user: {}", userId);
    //         return profileMapper.toProfileResponse(user, userDocuments);

    //     } catch (Exception e) {
    //         rollbackFileUploads(uploadedUrls);
    //         throw e;
    //     }
    // }

    // @Transactional(readOnly = true)
    // public ProfileResponse getProfile(String userId) {
    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     if (user.getDeleted()) {
    //         throw new BusinessException("USER_DELETED", "User account is deleted");
    //     }

    //     List<Document> documents = documentRepository.findByUserId(userId);
    //     return profileMapper.toProfileResponse(user, documents);
    // }

    /**
     * Simple JSON-based profile update (no file uploads).
     */
    // @Transactional
    // public ProfileResponse simpleUpdateProfile(String userId, UpdateProfileRequest request) {
    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     if (user.getDeleted()) {
    //         throw new BusinessException("USER_DELETED", "User account is deleted");
    //     }

    //     applyPartialUpdate(user, request);
    //     user.setUpdatedAt(LocalDateTime.now());
    //     user = userRepository.save(user);

    //     List<Document> documents = documentRepository.findByUserId(userId);
    //     log.info("Profile updated (JSON) for user: {}", userId);
    //     return profileMapper.toProfileResponse(user, documents);
    // }

    // @Transactional(readOnly = true)
    // public com.medsyncpro.dto.response.VerificationStatusResponse getVerificationStatus(String userId) {
    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
    //     String reviewNotes = request != null ? request.getReviewNotes() : null;
    //     LocalDateTime submittedAt = request != null ? request.getSubmittedAt() : null;
    //     LocalDateTime reviewedAt = request != null ? request.getReviewedAt() : null;

    //     List<Document> documents = documentRepository.findByUserId(userId);
    //     List<com.medsyncpro.dto.response.DocumentResponse> documentResponses = documents.stream()
    //             .map(profileMapper::toDocumentResponse)
    //             .collect(Collectors.toList());

    //     List<RequiredDocumentItem> requiredDocs = getRequiredDocuments(userId);

    //     return com.medsyncpro.dto.response.VerificationStatusResponse.builder()
    //             .status(userProfileHelper.getVerificationStatus(user))
    //             .submittedDocuments(documentResponses)
    //             .requiredDocuments(requiredDocs)
    //             .verificationNotes(reviewNotes)
    //             .submittedAt(submittedAt)
    //             .reviewedAt(reviewedAt)
    //             .build();
    // }

    // @Transactional
    // public com.medsyncpro.dto.response.VerificationStatusResponse uploadVerificationDocuments(
    //         String userId,
    //         List<MultipartFile> documents,
    //         Map<String, String> documentTypes) {

    //     User user = userRepository.findById(UUID.fromString(userId))
    //             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    //     List<String> uploadedUrls = new ArrayList<>();

    //     try {
    //         if (documents != null && !documents.isEmpty()) {
    //             handleDocumentUploads(user, documents, documentTypes, uploadedUrls);
    //         }

    //         user = userRepository.save(user);

    //         VerificationRequest request = verificationRequestRepository.findByUserId(userId).orElse(null);
    //         if (request == null) {
    //             request = new VerificationRequest();
    //             request.setUser(user);
    //         }
    //         request.setStatus(VerificationStatus.DOCUMENT_SUBMITTED);
    //         verificationRequestRepository.save(request);

    //         eventPublisher.publishEvent(new com.medsyncpro.event.DocumentSubmittedEvent(this, user));

    //     } catch (Exception e) {
    //         rollbackFileUploads(uploadedUrls);
    //         throw e;
    //     }

    //     return getVerificationStatus(userId);
    // }

    // ── Helpers ──

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

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }
    }

    // private List<Document> handleDocumentUploads(
    //         User user,
    //         List<MultipartFile> files,
    //         Map<String, String> documentTypes,
    //         List<String> uploadedUrls) {

    //     for (int i = 0; i < files.size(); i++) {
    //         MultipartFile file = files.get(i);
    //         if (file.isEmpty()) {
    //             continue;
    //         }

    //         String typeStr = documentTypes != null ? documentTypes.get("type_" + i) : null;
    //         DocumentTypeEntity docType = resolveDocumentType(typeStr);

    //         String url = fileStorageService.uploadDocument(file, user.getId().toString());
    //         uploadedUrls.add(url);

    //         Document document = new Document();
    //         document.setUserId(String.valueOf(user.getId()));
    //         document.setDocumentType(docType);
    //         document.setUrl(url);
    //         document.setFileName(file.getOriginalFilename());
    //         document.setFileSize(file.getSize());

    //         documentRepository.save(document);
    //     }

    //     return documentRepository.findByUserId(String.valueOf(user.getId()));
    // }

    // private DocumentTypeEntity resolveDocumentType(String typeStr) {
    //     if (typeStr == null || typeStr.trim().isEmpty()) {
    //         return documentTypeEntityRepository.findByCodeAndDeletedFalse("OTHER")
    //                 .orElseThrow(() -> new BusinessException("MISSING_DEFAULT_TYPE",
    //                         "Default document type 'OTHER' not found in database"));
    //     }
    //     return documentTypeEntityRepository.findByCodeAndDeletedFalse(typeStr.toUpperCase())
    //             .orElseThrow(() -> new BusinessException("INVALID_DOCUMENT_TYPE",
    //                     "Unknown document type code: " + typeStr));
    // }

    /**
     * Maps Role enum to UserModelType for verification document lookup.
     * Returns null for roles that don't participate in document verification
     * (ADMIN, PATIENT).
     */
    public static UserModelType roleToModelType(Role role) {
        return switch (role) {
            case DOCTOR -> UserModelType.DOCTOR;
            case PHARMACY -> UserModelType.PHARMACIST;
            case AGENT -> UserModelType.AGENT;
            default -> null;
        };
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
