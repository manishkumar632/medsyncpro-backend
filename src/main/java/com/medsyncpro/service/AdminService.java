package com.medsyncpro.service;

import com.medsyncpro.dto.AdminStatsResponse;
import com.medsyncpro.dto.AdminUserListResponse;
import com.medsyncpro.dto.DocumentResponse;
import com.medsyncpro.dto.PagedResponse;
import com.medsyncpro.entity.Document;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DocumentRepository;
import com.medsyncpro.repository.UserRepository;
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
        
        if (approved != null && hasSearch) {
            userPage = userRepository.findByRoleAndApprovedAndSearch(role, approved, search.trim(), pageable);
        } else if (approved != null) {
            userPage = userRepository.findByRoleAndApproved(role, approved, pageable);
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
     * Approve a user — sets approved=true.
     */
    @Transactional
    public AdminUserListResponse approveUser(String userId) {
        User user = findUserOrThrow(userId);
        user.setApproved(true);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} approved by admin", userId);
        return toAdminUserResponse(user);
    }
    
    /**
     * Reject a user — sets approved=false (they can re-submit).
     */
    @Transactional
    public AdminUserListResponse rejectUser(String userId) {
        User user = findUserOrThrow(userId);
        user.setApproved(false);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} rejected by admin", userId);
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
     * Activate a user — un-delete + approve.
     */
    @Transactional
    public AdminUserListResponse activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(false);
        user.setApproved(true);
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        log.info("User {} activated by admin", userId);
        return toAdminUserResponse(user);
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
                .approved(user.getApproved())
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
