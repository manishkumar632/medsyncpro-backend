package com.medsyncpro.service;

import com.medsyncpro.dto.AdminStatsResponse;
import com.medsyncpro.dto.AdminUserListResponse;
import com.medsyncpro.dto.DocumentResponse;
import com.medsyncpro.dto.PagedResponse;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DocumentRepository;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.repository.VerificationRequestRepository;
import com.medsyncpro.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final NotificationRepository notificationRepository;
    private final FirebasePushService firebasePushService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    
    @Transactional(readOnly = true)
    public AdminStatsResponse getUserStats() {
        long patients = userRepository.countByRoleAndDeletedFalse(Role.PATIENT);
        long doctors = userRepository.countByRoleAndDeletedFalse(Role.DOCTOR);
        long pharmacists = userRepository.countByRoleAndDeletedFalse(Role.PHARMACIST);
        long pendingApprovals = userRepository.countPendingApprovals();
        long totalUsers = patients + doctors + pharmacists;
        
        log.info("Admin stats fetched — patients:{}, doctors:{}, pharmacists:{}, pending:{}", 
                patients, doctors, pharmacists, pendingApprovals);
        
        return new AdminStatsResponse(patients, doctors, pharmacists, totalUsers, pendingApprovals);
    }
    
    /**
     * List users by role with optional filters for approval status and search.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserListResponse> listUsers(
            Role role, Boolean approved, String search, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;
        
        boolean hasSearch = search != null && !search.trim().isEmpty();
        
        if (approved != null) {
            VerificationStatus status = approved ? VerificationStatus.VERIFIED : VerificationStatus.UNVERIFIED;
            if (hasSearch) {
                userPage = userRepository.findByRoleAndVerificationStatusAndSearch(role, status, search.trim(), pageable);
            } else {
                userPage = userRepository.findByRoleAndVerificationStatus(role, status, pageable);
            }
        } else if (hasSearch) {
            userPage = userRepository.findByRoleAndSearch(role, search.trim(), pageable);
        } else {
            userPage = userRepository.findByRoleAndDeletedFalse(role, pageable);
        }
        
        List<AdminUserListResponse> content = userPage.getContent().stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
        
        return PagedResponse.<AdminUserListResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }
    
    /**
     * List pending approvals (email-verified, not approved, non-admin).
     */
    @Transactional(readOnly = true)
    public List<AdminUserListResponse> listPendingApprovals() {
        List<User> pending = userRepository.findPendingApprovals();
        return pending.stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Approve a user — sets verified. Publishes event for realtime notification.
     * Idempotent: if already VERIFIED, returns current state without re-publishing.
     */
    @Transactional
    public AdminUserListResponse approveUser(String userId) {
        User user = findUserOrThrow(userId);
        
        // Idempotent: skip if already verified
        if (user.getProfessionalVerificationStatus() == com.medsyncpro.entity.VerificationStatus.VERIFIED) {
            log.info("User {} already verified, skipping", userId);
            return toAdminUserResponse(user);
        }
        
        user.setProfessionalVerificationStatus(com.medsyncpro.entity.VerificationStatus.VERIFIED);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} approved by admin", userId);
        
        // Publish event → SSE notification to doctor + stored notification
        eventPublisher.publishEvent(new com.medsyncpro.event.VerificationDecisionEvent(
                this, user, com.medsyncpro.entity.VerificationStatus.VERIFIED, null));
        
        return toAdminUserResponse(user);
    }
    
    /**
     * Reject a user — sets rejected. Publishes event for realtime notification.
     * Idempotent: if already REJECTED, returns current state.
     */
    @Transactional
    public AdminUserListResponse rejectUser(String userId) {
        User user = findUserOrThrow(userId);
        
        if (user.getProfessionalVerificationStatus() == com.medsyncpro.entity.VerificationStatus.REJECTED) {
            log.info("User {} already rejected, skipping", userId);
            return toAdminUserResponse(user);
        }
        
        user.setProfessionalVerificationStatus(com.medsyncpro.entity.VerificationStatus.REJECTED);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} rejected by admin", userId);
        
        eventPublisher.publishEvent(new com.medsyncpro.event.VerificationDecisionEvent(
                this, user, com.medsyncpro.entity.VerificationStatus.REJECTED, null));
        
        return toAdminUserResponse(user);
    }
    
    /**
     * Suspend a user — soft-delete (sets deleted=true).
     */
    @Transactional
    public AdminUserListResponse suspendUser(String userId) {
        User user = findUserOrThrow(userId);
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} suspended by admin", userId);
        return toAdminUserResponse(user);
    }
    
    /**
     * Activate a user — un-delete + approve. Publishes event.
     */
    @Transactional
    public AdminUserListResponse activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(false);
        user.setProfessionalVerificationStatus(com.medsyncpro.entity.VerificationStatus.VERIFIED);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} activated by admin", userId);
        
        eventPublisher.publishEvent(new com.medsyncpro.event.VerificationDecisionEvent(
                this, user, com.medsyncpro.entity.VerificationStatus.VERIFIED, null));
        
        return toAdminUserResponse(user);
    }
    
    // ── FCM & Notifications ──
    
    @Transactional
    public void registerFcmToken(String adminId, String fcmToken) {
        User admin = findUserOrThrow(adminId);
        if (admin.getRole() != Role.ADMIN) {
             throw new IllegalArgumentException("Only admins can register dashboard FCM tokens");
        }
        admin.setFcmToken(fcmToken);
        userRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAdminNotifications(String adminId) {
        return notificationRepository.findByRecipientIdOrRecipientIdIsNullOrderByCreatedAtDesc(adminId);
    }

    @Transactional
    public void markNotificationAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // ── Verifications ──

    @Transactional(readOnly = true)
    public List<VerificationRequest> getAllVerificationRequests() {
        return verificationRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public VerificationRequest getVerificationRequest(String id) {
        return verificationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verification Request not found"));
    }

    @Transactional(readOnly = true)
    public com.medsyncpro.dto.AdminVerificationDetailResponse getVerificationDetail(String id) {
        VerificationRequest request = getVerificationRequest(id);
        User user = request.getUser();
        
        List<Document> docs = documentRepository.findByUserId(user.getId());
        List<DocumentResponse> docResponses = docs.stream()
                .map(d -> DocumentResponse.builder()
                        .id(d.getId())
                        .type(d.getType())
                        .url(d.getUrl())
                        .fileName(d.getFileName())
                        .fileSize(d.getFileSize())
                        .createdAt(d.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return com.medsyncpro.dto.AdminVerificationDetailResponse.builder()
                .id(request.getId())
                .status(request.getStatus())
                .reviewNotes(request.getReviewNotes())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .user(com.medsyncpro.dto.AdminVerificationDetailResponse.UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .profileImageUrl(user.getProfileImageUrl())
                        .createdAt(user.getCreatedAt())
                        .build())
                .documents(docResponses)
                .build();
    }

    @Transactional
    public void approveVerificationRequest(String id, String adminId) {
        VerificationRequest request = getVerificationRequest(id);
        if (request.getStatus() == VerificationStatus.VERIFIED) return;

        User user = request.getUser();
        user.setProfessionalVerificationStatus(VerificationStatus.VERIFIED);
        userRepository.save(user);

        request.setStatus(VerificationStatus.VERIFIED);
        request.setReviewedAt(LocalDateTime.now());
        verificationRequestRepository.save(request);

        // Notify user via Event
        eventPublisher.publishEvent(new com.medsyncpro.event.VerificationDecisionEvent(this, user, VerificationStatus.VERIFIED, null));
    }

    @Transactional
    public void rejectVerificationRequest(String id, String adminId, String comments) {
        VerificationRequest request = getVerificationRequest(id);
        if (request.getStatus() == VerificationStatus.REJECTED) return;

        User user = request.getUser();
        user.setProfessionalVerificationStatus(VerificationStatus.REJECTED);
        userRepository.save(user);

        request.setStatus(VerificationStatus.REJECTED);
        request.setReviewNotes(comments);
        request.setReviewedAt(LocalDateTime.now());
        verificationRequestRepository.save(request);

        // Notify user via Event
        eventPublisher.publishEvent(new com.medsyncpro.event.VerificationDecisionEvent(this, user, VerificationStatus.REJECTED, comments));
    }
    
    // ── Helpers ──
    
    private User findUserOrThrow(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getDeleted()) {
            throw new ResourceNotFoundException("User not found (deleted)");
        }
        return user;
    }
    
    private AdminUserListResponse toAdminUserResponse(User user) {
        List<Document> docs = documentRepository.findByUserId(user.getId());
        List<DocumentResponse> docResponses = docs.stream()
                .map(d -> DocumentResponse.builder()
                        .id(d.getId())
                        .type(d.getType())
                        .url(d.getUrl())
                        .fileName(d.getFileName())
                        .fileSize(d.getFileSize())
                        .createdAt(d.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return AdminUserListResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .professionalVerificationStatus(user.getProfessionalVerificationStatus())
                .emailVerified(user.getEmailVerified())
                .deleted(user.getDeleted())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .documents(docResponses)
                .build();
    }
}
