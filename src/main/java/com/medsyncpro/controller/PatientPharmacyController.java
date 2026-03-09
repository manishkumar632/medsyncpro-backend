package com.medsyncpro.controller;

import com.medsyncpro.dto.request.PharmacyMedicineRequestCreateRequest;
import com.medsyncpro.dto.response.PharmacyMedicineRequestResponse;
import com.medsyncpro.dto.response.PharmacySearchResponse;
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
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientPharmacyController {

    private final PharmacyWorkflowService pharmacyWorkflowService;
    private final Utils utils;

    @GetMapping("/api/patient/pharmacies")
    public ResponseEntity<ApiResponse<Page<PharmacySearchResponse>>> searchPharmacies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                pharmacyWorkflowService.searchPharmacies(q, location, PageRequest.of(page, Math.min(size, 100))),
                "Pharmacies fetched"));
    }

    @PostMapping("/api/patient/pharmacy-requests")
    public ResponseEntity<ApiResponse<PharmacyMedicineRequestResponse>> createRequest(
            Authentication authentication,
            @Valid @RequestBody PharmacyMedicineRequestCreateRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                pharmacyWorkflowService.createMedicineRequest(userId, request),
                "Pharmacy request submitted"));
    }

    @GetMapping("/api/patient/pharmacy-requests")
    public ResponseEntity<ApiResponse<Page<PharmacyMedicineRequestResponse>>> getMyRequests(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                pharmacyWorkflowService.getPatientRequests(userId, PageRequest.of(page, Math.min(size, 100))),
                "Patient pharmacy requests fetched"));
    }
}
