package com.medsyncpro.service;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.response.AdminUserListResponse;
import com.medsyncpro.dto.response.DocumentResponse;
import com.medsyncpro.dto.response.DocumentTypeResponse;
import com.medsyncpro.dto.response.PagedResponse;
import com.medsyncpro.entity.*;
import com.medsyncpro.event.VerificationResubmitEvent;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import com.medsyncpro.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

        private final VerificationRequestRepository verificationRequestRepository;
        private final NotificationRepository notificationRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final DocumentTypeRepository documentTypeRepository;
        private final AuditLogService auditLogService;
        private final UserRepository userRepository;
        private final DoctorRepository doctorRepository;
        private final DocumentRepository documentRepository;

        // ─────────────────────────────────────────────
        // USER LISTING
        // ─────────────────────────────────────────────

        @Transactional(readOnly = true)
        public PagedResponse<AdminUserListResponse> listUsers(
                        Role role, Boolean approved, String search, int page, int size) {

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

                Page<User> userPage = (search != null && !search.isBlank())
                                ? userRepository.findByRoleAndEmailContainingIgnoreCase(role, search.trim(), pageable)
                                : userRepository.findByRole(role, pageable);

                List<AdminUserListResponse> content = userPage.getContent().stream()
                                .map(user -> buildResponse(user, role))
                                .toList();

                return PagedResponse.<AdminUserListResponse>builder()
                                .content(content)
                                .page(page)
                                .size(size)
                                .totalElements(userPage.getTotalElements())
                                .totalPages(userPage.getTotalPages())
                                .build();
        }

        private AdminUserListResponse buildResponse(User user, Role role) {
                String name = null;
                String profileImageUrl = null;

                if (role == Role.DOCTOR) {
                        Optional<Doctor> doctorOpt = doctorRepository.findByUserId(user.getId());
                        if (doctorOpt.isPresent()) {
                                Doctor d = doctorOpt.get();
                                name = d.getName();
                                profileImageUrl = d.getProfileImage();
                        }
                }

                List<DocumentResponse> documents = documentRepository.findByUser(user)
                                .stream().map(DocumentResponse::from).toList();

                return AdminUserListResponse.builder()
                                .id(user.getId().toString())
                                .name(name)
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .role(user.getRole())
                                .emailVerified(user.isEmailVerified())
                                .deleted(false)
                                .profileImageUrl(profileImageUrl)
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .documents(documents)
                                .professionalVerificationStatus(deriveVerificationStatus(user))
                                .build();
        }

        private String deriveVerificationStatus(User user) {
                return verificationRequestRepository
                                .findTopByUserOrderByCreatedAtDesc(user)
                                .map(r -> r.getStatus().name())
                                .orElse(user.isEmailVerified() ? "EMAIL_VERIFIED" : "UNVERIFIED");
        }

        // ─────────────────────────────────────────────
        // USER-SCOPED VERIFICATION ACTIONS
        // ─────────────────────────────────────────────

        /** Approves the latest verification request for the given userId. */
        @Transactional
        public void approveVerificationByUserId(UUID userId) {
                VerificationRequest request = findVerificationRequest(userId);
                approveVerificationRequest(request.getId());
        }

        /** Rejects the latest verification request for the given userId. */
        @Transactional
        public void rejectVerificationByUserId(UUID userId, String comments) {
                VerificationRequest request = findVerificationRequest(userId);
                rejectVerificationRequest(request.getId(), comments);
        }


        @Transactional
        public void requestResubmitByUserId(UUID userId, String comment, List<String> documentTypeCodes) {
                VerificationRequest request = findVerificationRequest(userId);

                request.setStatus(VerificationStatus.RESUBMIT_REQUESTED);
                request.setReviewNotes(comment);
                request.setRequestedDocumentTypes(
                                documentTypeCodes == null ? "" : String.join(",", documentTypeCodes));
                request.setReviewedAt(LocalDateTime.now());
                verificationRequestRepository.save(request);

                // Fire event — VerificationEventHandler will send notification + email
                eventPublisher.publishEvent(
                                new VerificationResubmitEvent(
                                                this,
                                                request.getUser(),
                                                comment,
                                                documentTypeCodes));

                auditLogService.logAction(userId, "RESUBMIT_REQUESTED",
                                "Admin requested re-upload of: "
                                                + (documentTypeCodes != null ? documentTypeCodes : "[]"));
        }

        // ─────────────────────────────────────────────
        // VERIFICATION ACTIONS (by verificationRequestId)
        // ─────────────────────────────────────────────

        @Transactional
        public void approveVerificationRequest(String requestId) {
                VerificationRequest request = verificationRequestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));

                if (request.getStatus() == VerificationStatus.VERIFIED)
                        return;

                request.setStatus(VerificationStatus.VERIFIED);
                request.setReviewedAt(LocalDateTime.now());
                verificationRequestRepository.save(request);

                eventPublisher.publishEvent(
                                new com.medsyncpro.event.VerificationDecisionEvent(
                                                this, request.getUser(), VerificationStatus.VERIFIED, null));

                auditLogService.logAction(request.getUser().getId(), "VERIFICATION_APPROVED",
                                "Admin approved verification request: " + requestId);
        }

        @Transactional
        public void rejectVerificationRequest(String requestId, String comments) {
                VerificationRequest request = verificationRequestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));

                if (request.getStatus() == VerificationStatus.REJECTED)
                        return;

                request.setStatus(VerificationStatus.REJECTED);
                request.setReviewNotes(comments);
                request.setReviewedAt(LocalDateTime.now());
                verificationRequestRepository.save(request);

                eventPublisher.publishEvent(
                                new com.medsyncpro.event.VerificationDecisionEvent(
                                                this, request.getUser(), VerificationStatus.REJECTED, comments));

                auditLogService.logAction(request.getUser().getId(), "VERIFICATION_REJECTED",
                                "Admin rejected verification request: " + requestId
                                                + ". Reason: " + comments);
        }

        @Transactional(readOnly = true)
        public List<VerificationRequest> getAllVerificationRequests() {
                return verificationRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        // ─────────────────────────────────────────────
        // NOTIFICATIONS
        // ─────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<Notification> getAdminNotifications(String adminId) {
                return notificationRepository
                                .findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(adminId);
        }

        @Transactional
        public void markNotificationAsRead(String notificationId) {
                Notification notification = notificationRepository.findById(notificationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
                notification.setIsRead(true);
                notificationRepository.save(notification);
        }

        // ─────────────────────────────────────────────
        // DOCUMENT TYPE MANAGEMENT
        // ─────────────────────────────────────────────

        public ApiResponse<DocumentType> addDocumentType(DocumentTypeRequest request) {
                if (documentTypeRepository.existsByCodeAndRole(request.getCode(), request.getRole())) {
                        return ApiResponse.error(
                                        "Document type with code '" + request.getCode() + "' already exists.");
                }
                DocumentType documentType = DocumentType.builder()
                                .name(request.getName()).code(request.getCode())
                                .role(request.getRole()).required(request.isRequired())
                                .active(request.isActive()).description(request.getDescription())
                                .displayOrder(request.getDisplayOrder()).build();
                documentTypeRepository.save(documentType);
                return ApiResponse.success(documentType,
                                request.getName() + " document type added successfully.");
        }

        @Transactional
        public void deleteDocumentType(UUID id) {
                DocumentType dt = documentTypeRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document type not found with id: " + id));
                try {
                        documentTypeRepository.delete(dt);
                        documentTypeRepository.flush();
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        throw new IllegalStateException(
                                        "Cannot delete '" + dt.getName()
                                                        + "' because existing user documents reference it.");
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
                return DocumentTypeResponse.from(dt);
        }

        @Transactional
        public DocumentTypeResponse toggleDocumentTypeActive(UUID id) {
                DocumentType dt = documentTypeRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document type not found with id: " + id));
                dt.setActive(!dt.isActive());
                documentTypeRepository.save(dt);
                return DocumentTypeResponse.from(dt);
        }

        // ─────────────────────────────────────────────
        // PRIVATE HELPERS
        // ─────────────────────────────────────────────

        private VerificationRequest findVerificationRequest(UUID userId) {
                return verificationRequestRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "No verification request found for user: " + userId));
        }
}