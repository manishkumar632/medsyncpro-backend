package com.medsyncpro.controller;

import com.medsyncpro.dto.request.HealthTrackerEntryRequest;
import com.medsyncpro.dto.response.HealthTrackerEntryResponse;
import com.medsyncpro.entity.HealthMetricType;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.HealthTrackerService;
import com.medsyncpro.utils.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient/health-tracker")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientHealthTrackerController {

    private final HealthTrackerService healthTrackerService;
    private final Utils utils;

    @PostMapping
    public ResponseEntity<ApiResponse<HealthTrackerEntryResponse>> addEntry(
            Authentication authentication,
            @Valid @RequestBody HealthTrackerEntryRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                healthTrackerService.addEntry(userId, request),
                "Health tracker entry saved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthTrackerEntryResponse>>> getEntries(
            Authentication authentication,
            @RequestParam(required = false) HealthMetricType metricType) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                healthTrackerService.getEntries(userId, metricType),
                "Health tracker entries fetched"));
    }
}
