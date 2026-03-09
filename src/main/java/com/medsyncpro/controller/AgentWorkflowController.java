package com.medsyncpro.controller;

import com.medsyncpro.dto.request.PharmacyRequestStatusUpdateRequest;
import com.medsyncpro.dto.response.PharmacyMedicineRequestResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.PharmacyWorkflowService;
import com.medsyncpro.utils.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/agent/requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class AgentWorkflowController {

    private final PharmacyWorkflowService pharmacyWorkflowService;
    private final Utils utils;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PharmacyMedicineRequestResponse>>> getAssignedRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                pharmacyWorkflowService.getAgentRequests(userId, PageRequest.of(page, Math.min(size, 100))),
                "Assigned delivery requests fetched"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PharmacyMedicineRequestResponse>> updateDeliveryStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody PharmacyRequestStatusUpdateRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                pharmacyWorkflowService.updateAgentRequestStatus(userId, id, request),
                "Delivery status updated"));
    }
}
