package com.medsyncpro.controller;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.request.ResubmitRequestDTO;
import com.medsyncpro.dto.request.VerificationActionRequest;
import com.medsyncpro.dto.response.*;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Notification;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.AdminService;
import com.medsyncpro.service.OnlyQueryService;
import com.medsyncpro.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final OnlyQueryService queryService;

    // ─────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(
                queryService.getStats(), "Admin statistics retrieved successfully"));
    }

    // ─────────────────────────────────────────────
    // DOCUMENT TYPE MANAGEMENT
    // ─────────────────────────────────────────────

    @PostMapping("/add-new-document-type")
    public ResponseEntity<ApiResponse<DocumentType>> addDocumentType(
            @Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.ok(adminService.addDocumentType(request));
    }

    @PatchMapping("/document-types/mapping/{id}/toggle-required")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> toggleRequired(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.toggleDocumentTypeRequired(id), "Required status updated"));
    }

    @PatchMapping("/document-types/mapping/{id}/toggle-active")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.toggleDocumentTypeActive(id), "Active status updated"));
    }

    @DeleteMapping("/document-types/mapping/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable UUID id) {
        adminService.deleteDocumentType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document type removed successfully"));
    }

    // ─────────────────────────────────────────────
    // USER LISTING
    // ─────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserListResponse>>> listUsers(
            @RequestParam Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        PagedResponse<AdminUserListResponse> result = adminService.listUsers(role, null, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Users retrieved successfully"));
    }

    // ─────────────────────────────────────────────
    // USER-SCOPED VERIFICATION ACTIONS
    // ─────────────────────────────────────────────

    /**
     * POST /api/admin/users/{userId}/approve-verification
     * Approves the most recent verification request for the given user.
     * Triggers: in-app notification + FCM push + congratulations email.
     */
    @PostMapping("/users/{userId}/approve-verification")
    public ResponseEntity<ApiResponse<Void>> approveVerificationByUserId(
            @PathVariable UUID userId) {
        adminService.approveVerificationByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification approved successfully"));
    }

    /**
     * POST /api/admin/users/{userId}/reject-verification
     * Body: { "comment": "reason text" }
     * Triggers: in-app notification + FCM push + rejection email.
     */
    @PostMapping("/users/{userId}/reject-verification")
    public ResponseEntity<ApiResponse<Void>> rejectVerificationByUserId(
            @PathVariable UUID userId,
            @RequestBody VerificationActionRequest request) {
        adminService.rejectVerificationByUserId(userId, request.getComment());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification rejected successfully"));
    }

    @PostMapping("/users/{userId}/request-resubmit")
    public ResponseEntity<ApiResponse<Void>> requestResubmit(
            @PathVariable UUID userId,
            @RequestBody ResubmitRequestDTO request) {
        adminService.requestResubmitByUserId(
                userId, request.getComment(), request.getDocumentTypeCodes());
        return ResponseEntity.ok(ApiResponse.success(null, "Re-upload request sent to doctor"));
    }

    // ─────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            Authentication authentication) {
        String email = authentication.getName();
        User admin = userService.getUserByEmail(email);
        List<Notification> notifications = adminService.getAdminNotifications(
                String.valueOf(admin.getId()));
        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications fetched successfully"));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable String id) {
        adminService.markNotificationAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked read"));
    }

    // ─────────────────────────────────────────────
    // VERIFICATION REQUESTS (by verificationRequestId)
    // ─────────────────────────────────────────────

    @GetMapping("/verifications")
    public ResponseEntity<ApiResponse<List<VerificationRequest>>> getAllVerifications() {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAllVerificationRequests(), "Verification requests fetched"));
    }

    @PostMapping("/verifications/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVerification(@PathVariable String id) {
        adminService.approveVerificationRequest(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification approved successfully"));
    }

    @PostMapping("/verifications/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectVerification(
            @PathVariable String id,
            @RequestBody VerificationActionRequest request) {
        adminService.rejectVerificationRequest(id, request.getComment());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification rejected successfully"));
    }
}