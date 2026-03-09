package com.medsyncpro.controller;

import com.medsyncpro.dto.request.CreateMedicationScheduleRequest;
import com.medsyncpro.dto.request.DoseActionRequest;
import com.medsyncpro.dto.request.UpdateMedicationScheduleRequest;
import com.medsyncpro.dto.response.MedicationAdherenceSummaryResponse;
import com.medsyncpro.dto.response.MedicationDoseLogResponse;
import com.medsyncpro.dto.response.MedicationScheduleResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.MedicationWorkflowService;
import com.medsyncpro.utils.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient/medications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientMedicationController {

    private final MedicationWorkflowService medicationWorkflowService;
    private final Utils utils;

    @PostMapping("/schedules")
    public ResponseEntity<ApiResponse<MedicationScheduleResponse>> createSchedule(
            Authentication authentication,
            @Valid @RequestBody CreateMedicationScheduleRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.createSchedule(userId, request),
                "Medication schedule created"));
    }

    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<MedicationScheduleResponse>>> getSchedules(Authentication authentication) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.getSchedules(userId),
                "Medication schedules fetched"));
    }

    @PatchMapping("/schedules/{id}")
    public ResponseEntity<ApiResponse<MedicationScheduleResponse>> updateSchedule(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody UpdateMedicationScheduleRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.updateSchedule(userId, id, request),
                "Medication schedule updated"));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateSchedule(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        medicationWorkflowService.deactivateSchedule(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Medication schedule deactivated"));
    }

    @GetMapping("/dose-logs")
    public ResponseEntity<ApiResponse<Page<MedicationDoseLogResponse>>> getDoseLogs(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.getDoseLogs(userId, PageRequest.of(page, Math.min(size, 100))),
                "Dose logs fetched"));
    }

    @PatchMapping("/dose-logs/{id}/taken")
    public ResponseEntity<ApiResponse<MedicationDoseLogResponse>> markTaken(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) DoseActionRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.markDoseTaken(userId, id, request),
                "Dose marked as taken"));
    }

    @PatchMapping("/dose-logs/{id}/snooze")
    public ResponseEntity<ApiResponse<MedicationDoseLogResponse>> snoozeDose(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) DoseActionRequest request) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.snoozeDose(userId, id, request),
                "Dose reminder snoozed"));
    }

    @GetMapping("/adherence")
    public ResponseEntity<ApiResponse<MedicationAdherenceSummaryResponse>> getAdherence(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        UUID userId = utils.getUserFromAuth(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success(
                medicationWorkflowService.getAdherenceSummary(userId, days),
                "Adherence summary fetched"));
    }
}
