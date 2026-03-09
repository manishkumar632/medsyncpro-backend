package com.medsyncpro.controller;

import com.medsyncpro.dto.response.DoctorAdherenceAlertResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.MedicationWorkflowService;
import com.medsyncpro.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/adherence")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAdherenceController {

    private final MedicationWorkflowService medicationWorkflowService;
    private final Utils utils;

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<DoctorAdherenceAlertResponse>>> getAdherenceAlerts(
            Authentication authentication) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.getDoctorAdherenceAlerts(userId),
                "Doctor adherence alerts fetched"));
    }
}
