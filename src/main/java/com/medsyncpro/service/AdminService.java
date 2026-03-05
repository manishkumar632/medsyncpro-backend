package com.medsyncpro.service;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.response.*;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.utils.UserProfileHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

        private final UserRepository userRepository;
        private final UserProfileHelper userProfileHelper;
        private final DocumentRepository documentRepository;
        private final VerificationRequestRepository verificationRequestRepository;
        private final NotificationRepository notificationRepository;
        private final FirebasePushService firebasePushService;
        private final ApplicationEventPublisher eventPublisher;
        private final DocumentTypeRepository documentTypeRepository;


        // ─────────────────────────────────────────────
        // USER LISTING (ROLE + SEARCH ONLY)
        // ─────────────────────────────────────────────

        // @Transactional(readOnly = true)
        // public PagedResponse<AdminUserListResponse> listUsers(
        //                 Role role, Boolean approved, String search, int page, int size) {

        //         Pageable pageable = PageRequest.of(page, size,
        //                         Sort.by(Sort.Direction.DESC, "createdAt"));

        //         Page<User> userPage;

        //         boolean hasSearch = search != null && !search.trim().isEmpty();

        //         if (hasSearch) {
        //                 userPage = userRepository.findByRoleAndSearch(
        //                                 role, search.trim(), pageable);
        //         } else {
        //                 userPage = userRepository.findByRoleAndDeletedFalse(
        //                                 role, pageable);
        //         }

        //         List<AdminUserListResponse> content = userPage.getContent().stream()
        //                         .map(this::toAdminUserResponse)
        //                         .collect(Collectors.toList());

        //         return PagedResponse.<AdminUserListResponse>builder()
        //                         .content(content)
        //                         .page(userPage.getNumber())
        //                         .size(userPage.getSize())
        //                         .totalElements(userPage.getTotalElements())
        //                         .totalPages(userPage.getTotalPages())
        //                         .build();
        // }

        // ─────────────────────────────────────────────
        // PENDING APPROVALS (Via VerificationRequest)
        // ─────────────────────────────────────────────

        // @Transactional(readOnly = true)
        // public List<AdminUserListResponse> listPendingApprovals() {

        //         List<VerificationRequest> pendingRequests = verificationRequestRepository
        //                         .findByStatusInOrderByCreatedAtDesc(
        //                                         List.of(
        //                                                         VerificationStatus.UNDER_REVIEW,
        //                                                         VerificationStatus.DOCUMENT_SUBMITTED));

        //         return pendingRequests.stream()
        //                         .map(VerificationRequest::getUser)
        //                         .map(this::toAdminUserResponse)
        //                         .collect(Collectors.toList());
        // }

        // ─────────────────────────────────────────────
        // USER ACTIONS
        // ─────────────────────────────────────────────

        // @Transactional
        // public AdminUserListResponse suspendUser(String userId) {
        //         User user = findUserOrThrow(userId);
        //         user.setDeleted(true);
        //         user.setUpdatedAt(LocalDateTime.now());
        //         userRepository.save(user);

        //         log.info("User {} suspended by admin", userId);
        //         return toAdminUserResponse(user);
        // }

        // @Transactional
        // public AdminUserListResponse activateUser(String userId) {
        //         User user = findUserOrThrow(userId);
        //         user.setDeleted(false);
        //         user.setUpdatedAt(LocalDateTime.now());
        //         userRepository.save(user);

        //         log.info("User {} activated by admin", userId);
        //         return toAdminUserResponse(user);
        // }

        // ─────────────────────────────────────────────
        // VERIFICATION ACTIONS
        // ─────────────────────────────────────────────

        @Transactional
        public void approveVerificationRequest(String requestId) {

                VerificationRequest request = verificationRequestRepository
                                .findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));

                if (request.getStatus() == VerificationStatus.VERIFIED)
                        return;

                request.setStatus(VerificationStatus.VERIFIED);
                request.setReviewedAt(LocalDateTime.now());
                verificationRequestRepository.save(request);

                eventPublisher.publishEvent(
                                new com.medsyncpro.event.VerificationDecisionEvent(
                                                this,
                                                request.getUser(),
                                                VerificationStatus.VERIFIED,
                                                null));
        }

        @Transactional
        public void rejectVerificationRequest(String requestId, String comments) {

                VerificationRequest request = verificationRequestRepository
                                .findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));

                if (request.getStatus() == VerificationStatus.REJECTED)
                        return;

                request.setStatus(VerificationStatus.REJECTED);
                request.setReviewNotes(comments);
                request.setReviewedAt(LocalDateTime.now());
                verificationRequestRepository.save(request);

                eventPublisher.publishEvent(
                                new com.medsyncpro.event.VerificationDecisionEvent(
                                                this,
                                                request.getUser(),
                                                VerificationStatus.REJECTED,
                                                comments));
        }

        // ─────────────────────────────────────────────
        // NOTIFICATIONS
        // ─────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<Notification> getAdminNotifications(String adminId) {
                return notificationRepository
                                .findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(
                                                UUID.fromString(adminId));
        }

        @Transactional
        public void markNotificationAsRead(String notificationId) {
                Notification notification = notificationRepository
                                .findById(notificationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

                notification.setIsRead(true);
                notificationRepository.save(notification);
        }

        @Transactional(readOnly = true)
        public List<VerificationRequest> getAllVerificationRequests() {
                return verificationRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        // @Transactional(readOnly = true)
        // public AdminVerificationDetailResponse getVerificationDetail(String id) {

        //         VerificationRequest request = verificationRequestRepository.findById(id)
        //                         .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));

        //         User user = request.getUser();

        //         List<Document> docs = documentRepository.findByUserId(String.valueOf(user.getId()));

        //         List<DocumentResponse> docResponses = docs.stream()
        //                         .map(d -> DocumentResponse.builder()
        //                                         .id(d.getId())
        //                                         .documentTypeId(d.getDocumentType().getId())
        //                                         .typeName(d.getDocumentType().getName())
        //                                         .typeCode(d.getDocumentType().getCode())
        //                                         .url(d.getUrl())
        //                                         .fileName(d.getFileName())
        //                                         .fileSize(d.getFileSize())
        //                                         .createdAt(d.getCreatedAt())
        //                                         .build())
        //                         .toList();

        //         return AdminVerificationDetailResponse.builder()
        //                         .id(request.getId())
        //                         .status(request.getStatus())
        //                         .reviewNotes(request.getReviewNotes())
        //                         .createdAt(request.getCreatedAt())
        //                         .updatedAt(request.getUpdatedAt())
        //                         .submittedAt(request.getSubmittedAt())
        //                         .reviewedAt(request.getReviewedAt())
        //                         .user(AdminVerificationDetailResponse.UserSummary.builder()
        //                                         .id(String.valueOf(user.getId()))
        //                                         .name(userProfileHelper.getName(user))
        //                                         .email(user.getEmail())
        //                                         .phone(user.getPhone())
        //                                         .role(user.getRole().name())
        //                                         .profileImageUrl(userProfileHelper.getProfileImage(user))
        //                                         .createdAt(user.getCreatedAt())
        //                                         .build())
        //                         .documents(docResponses)
        //                         .build();
        // }

        // ─────────────────────────────────────────────
        // HELPERS
        // ─────────────────────────────────────────────

        private User findUserOrThrow(String userId) {
                User user = userRepository.findById(UUID.fromString(userId))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                if (user.getDeleted()) {
                        throw new ResourceNotFoundException("User not found (deleted)");
                }

                return user;
        }

        // private AdminUserListResponse toAdminUserResponse(User user) {

        //         List<Document> docs = documentRepository.findByUserId(String.valueOf(user.getId()));

        //         List<DocumentResponse> docResponses = docs.stream()
        //                         .map(d -> DocumentResponse.builder()
        //                                         .id(d.getId())
        //                                         .documentTypeId(d.getDocumentType().getId())
        //                                         .typeName(d.getDocumentType().getName())
        //                                         .typeCode(d.getDocumentType().getCode())
        //                                         .url(d.getUrl())
        //                                         .fileName(d.getFileName())
        //                                         .fileSize(d.getFileSize())
        //                                         .createdAt(d.getCreatedAt())
        //                                         .build())
        //                         .collect(Collectors.toList());

        //         return AdminUserListResponse.builder()
        //                         .id(String.valueOf(user.getId()))
        //                         .name(userProfileHelper.getName(user))
        //                         .email(user.getEmail())
        //                         .phone(user.getPhone())
        //                         .role(user.getRole())
        //                         .emailVerified(user.isEmailVerified())
        //                         .deleted(user.getDeleted())
        //                         .profileImageUrl(userProfileHelper.getProfileImage(user))
        //                         .gender(userProfileHelper.getGender(user))
        //                         .createdAt(user.getCreatedAt())
        //                         .updatedAt(user.getUpdatedAt())
        //                         .documents(docResponses)
        //                         .build();
        // }

        public ApiResponse<DocumentType> addDocumentType(DocumentTypeRequest request) {
                boolean docTypeExist = documentTypeRepository.existsByCodeAndRole(request.getCode(), request.getRole());

                if (docTypeExist) {
                        return ApiResponse.error("Document type with code '" + request.getCode() + "' already exists.");
                }

                DocumentType documentType = DocumentType.builder()
                                .name(request.getName())
                                .code(request.getCode())
                                .role(request.getRole())
                                .required(request.isRequired())
                                .active(request.isActive())
                                .description(request.getDescription())
                                .displayOrder(request.getDisplayOrder())
                                .build();

                documentTypeRepository.save(documentType);
                return ApiResponse.success(documentType,
                                request.getName() + " document type added successfully.");
        }
        
        /**
         * Hard-deletes a document type only when no user documents reference it.
         * If the type is in use, the DB constraint (or explicit guard) prevents
         * deletion
         * and a clear message is returned — business logic stays server-side.
         */
        @Transactional
        public void deleteDocumentType(UUID id) {
                DocumentType dt = documentTypeRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document type not found with id: " + id));

                try {
                        documentTypeRepository.delete(dt);
                        documentTypeRepository.flush(); // surface FK constraint violations immediately
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        throw new IllegalStateException(
                                        "Cannot delete '" + dt.getName()
                                                        + "' because existing user documents reference it. " +
                                                        "Deactivate it instead to prevent new submissions.");
                }

                log.info("Document type '{}' ({}) deleted by admin", dt.getName(), id);
        }

        @Transactional
        public DocumentTypeResponse toggleDocumentTypeRequired(UUID id) {
                DocumentType dt = documentTypeRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document type not found with id: " + id));

                dt.setRequired(!dt.isRequired());
                documentTypeRepository.save(dt);

                log.info("Document type '{}' required → {}", dt.getName(), dt.isRequired());
                return DocumentTypeResponse.from(dt);
        }

        @Transactional
        public DocumentTypeResponse toggleDocumentTypeActive(UUID id) {
                DocumentType dt = documentTypeRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document type not found with id: " + id));

                dt.setActive(!dt.isActive());
                documentTypeRepository.save(dt);

                log.info("Document type '{}' active → {}", dt.getName(), dt.isActive());
                return DocumentTypeResponse.from(dt);
        }
}