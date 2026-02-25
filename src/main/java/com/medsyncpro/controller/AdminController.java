package com.medsyncpro.controller;

import com.medsyncpro.dto.AdminStatsResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * GET /api/admin/stats
     * Returns total count of patients, doctors, pharmacists, total users, and pending approvals.
     * 
     * Edge cases handled:
     * - Unauthenticated: 401 (JWT filter)
     * - Authenticated but not ADMIN: 403 (@PreAuthorize)
     * - Soft-deleted users: excluded from counts
     * - Expired/blacklisted tokens: 401 (JWT filter)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Admin statistics retrieved successfully"));
    }
}
