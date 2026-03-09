package com.medsyncpro.controller;

import com.medsyncpro.dto.request.AppointmentRequest;
import com.medsyncpro.dto.response.AppointmentResponse;
import com.medsyncpro.dto.response.SlotResponse;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final PatientService patientService;
    private final UserRepository userRepository;

    // ─── Book Appointment ─────────────────────────────────────────────────

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            Authentication authentication,
            @RequestBody AppointmentRequest request) {
        try {
            User user = getUser(authentication);
            AppointmentResponse response = patientService.bookAppointment(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(response, "Appointment booked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Patient's Appointments ───────────────────────────────────────────

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getMyAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUser(authentication);
        Page<AppointmentResponse> appointments = patientService.getPatientAppointments(
                user.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(appointments, "Appointments retrieved"));
    }

    // ─── Cancel Appointment ───────────────────────────────────────────────

    @PatchMapping("/appointments/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            Authentication authentication,
            @PathVariable UUID id) {
        try {
            User user = getUser(authentication);
            AppointmentResponse response = patientService.cancelAppointment(user.getId(), id);
            return ResponseEntity.ok(ApiResponse.success(response, "Appointment cancelled"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Available Slots ──────────────────────────────────────────────────

    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
            @PathVariable UUID doctorId,
            @RequestParam(required = false, defaultValue = "VIDEO") String type) {
        List<SlotResponse> slots = patientService.getAvailableSlots(doctorId, type);
        return ResponseEntity.ok(ApiResponse.success(slots, "Available slots retrieved"));
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyPrescriptions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        User user = getUser(authentication);
        Page<Map<String, Object>> prescriptions = patientService.getPatientPrescriptions(user.getId(),
                PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(prescriptions, "Prescriptions retrieved successfully"));
    }
}
