package com.medsyncpro.controller;

import com.medsyncpro.dto.request.DocumentTypeRequest;
import com.medsyncpro.dto.request.VerificationActionRequest;
import com.medsyncpro.dto.response.*;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Notification;
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
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final OnlyQueryService queryService;

    @GetMapping("/stats")   // get counts of patient, doctors, pharmacy and agent (verified and unverified)
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = queryService.getStats();
        return ResponseEntity.ok(
                ApiResponse.success(stats, "Admin statistics retrieved successfully"));
    }

    @PostMapping("/add-new-document-type")  // add new document type
    public ResponseEntity<ApiResponse<DocumentType>> addDocumentType(@Valid @RequestBody DocumentTypeRequest request) {
        ApiResponse<DocumentType> documentType = adminService.addDocumentType(request);
        return ResponseEntity.ok(documentType);
    }

    @PatchMapping("/document-types/mapping/{id}/toggle-required")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> toggleRequired(
            @PathVariable UUID id) {
        DocumentTypeResponse updated = adminService.toggleDocumentTypeRequired(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Required status updated"));
    }

    /** Flips the required flag — true → false or false → true. */
    @PatchMapping("/document-types/mapping/{id}/toggle-active")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> toggleActive(
            @PathVariable UUID id) {
        DocumentTypeResponse updated = adminService.toggleDocumentTypeActive(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Active status updated"));
    }
    
    /** Activates or deactivates a document type without deleting it. */
    @DeleteMapping("/document-types/mapping/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable UUID id) {
        adminService.deleteDocumentType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document type removed successfully"));
    }

    // ─────────────────────────────────────────────
    // USER LISTING
    // ─────────────────────────────────────────────

//     @GetMapping("/users")
//     public ResponseEntity<ApiResponse<PagedResponse<AdminUserListResponse>>> listUsers(
//             @RequestParam Role role,
//             @RequestParam(required = false) Boolean approved,
//             @RequestParam(required = false) String search,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "20") int size) {

//         PagedResponse<AdminUserListResponse> result = adminService.listUsers(role, approved, search, page, size);

//         return ResponseEntity.ok(
//                 ApiResponse.success(result, "Users retrieved successfully"));
//     }

//     @GetMapping("/users/pending")
//     public ResponseEntity<ApiResponse<List<AdminUserListResponse>>> listPendingApprovals() {
//         List<AdminUserListResponse> pending = adminService.listPendingApprovals();

//         return ResponseEntity.ok(
//                 ApiResponse.success(pending, "Pending approvals retrieved"));
//     }

    // ─────────────────────────────────────────────
    // USER STATUS ACTIONS
    // ─────────────────────────────────────────────

//     @PatchMapping("/users/{id}/suspend")
//     public ResponseEntity<ApiResponse<AdminUserListResponse>> suspendUser(
//             @PathVariable String id) {

//         AdminUserListResponse user = adminService.suspendUser(id);

//         return ResponseEntity.ok(
//                 ApiResponse.success(user, "User suspended"));
//     }

//     @PatchMapping("/users/{id}/activate")
//     public ResponseEntity<ApiResponse<AdminUserListResponse>> activateUser(
//             @PathVariable String id) {

//         AdminUserListResponse user = adminService.activateUser(id);

//         return ResponseEntity.ok(
//                 ApiResponse.success(user, "User activated"));
//     }

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

        return ResponseEntity.ok(
                ApiResponse.success(notifications, "Notifications fetched successfully"));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(
            @PathVariable String id) {

        adminService.markNotificationAsRead(id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Notification marked read"));
    }

    // ─────────────────────────────────────────────
    // VERIFICATION REQUESTS
    // ─────────────────────────────────────────────

    @GetMapping("/verifications")
    public ResponseEntity<ApiResponse<List<VerificationRequest>>> getAllVerifications() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminService.getAllVerificationRequests(),
                        "Verification requests fetched"));
    }

//     @GetMapping("/verifications/{id}")
//     public ResponseEntity<ApiResponse<AdminVerificationDetailResponse>> getVerification(
//             @PathVariable String id) {

//         return ResponseEntity.ok(
//                 ApiResponse.success(
//                         adminService.getVerificationDetail(id),
//                         "Verification details fetched"));
//     }

    @PostMapping("/verifications/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVerification(
            @PathVariable String id) {

        adminService.approveVerificationRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Verification approved successfully"));
    }

    @PostMapping("/verifications/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectVerification(
            @PathVariable String id,
            @RequestBody VerificationActionRequest request) {

        adminService.rejectVerificationRequest(id, request.getComment());

        return ResponseEntity.ok(
                ApiResponse.success(null, "Verification rejected successfully"));
    }
}