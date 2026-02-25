package com.medsyncpro.controller;

import com.medsyncpro.dto.AdminStatsResponse;
import com.medsyncpro.dto.AdminUserListResponse;
import com.medsyncpro.dto.PagedResponse;
import com.medsyncpro.entity.Role;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * GET /api/admin/stats
     * Dashboard KPI stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Admin statistics retrieved successfully"));
    }
    
    /**
     * GET /api/admin/users?role=DOCTOR&approved=false&search=priya&page=0&size=20
     * List users with optional filters.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserListResponse>>> listUsers(
            @RequestParam Role role,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PagedResponse<AdminUserListResponse> result = adminService.listUsers(role, approved, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Users retrieved successfully"));
    }
    
    /**
     * GET /api/admin/users/pending
     * List all pending approvals (email-verified, not yet approved, non-admin).
     */
    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<AdminUserListResponse>>> listPendingApprovals() {
        List<AdminUserListResponse> pending = adminService.listPendingApprovals();
        return ResponseEntity.ok(ApiResponse.success(pending, "Pending approvals retrieved"));
    }
    
    /**
     * PATCH /api/admin/users/{id}/approve
     */
    @PatchMapping("/users/{id}/approve")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> approveUser(@PathVariable String id) {
        AdminUserListResponse user = adminService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User approved successfully"));
    }
    
    /**
     * PATCH /api/admin/users/{id}/reject
     */
    @PatchMapping("/users/{id}/reject")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> rejectUser(@PathVariable String id) {
        AdminUserListResponse user = adminService.rejectUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User rejected"));
    }
    
    /**
     * PATCH /api/admin/users/{id}/suspend
     */
    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> suspendUser(@PathVariable String id) {
        AdminUserListResponse user = adminService.suspendUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User suspended"));
    }
    
    /**
     * PATCH /api/admin/users/{id}/activate
     */
    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> activateUser(@PathVariable String id) {
        AdminUserListResponse user = adminService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User activated"));
    }
}
