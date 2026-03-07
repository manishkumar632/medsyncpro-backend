package com.medsyncpro.controller;

import com.medsyncpro.dto.response.PrescriptionResponse;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PHARMACY')")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final UserRepository userRepository;

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Page<PrescriptionResponse>>> getPrescriptions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        @SuppressWarnings("unused")
        User user = getUser(authentication);
        // Pharmacies currently can see all published prescriptions (or just search for
        // them in future)
        // For now, let's just return all prescriptions for demonstration, or we can add
        // search by patient ID
        Page<PrescriptionResponse> prescriptions = pharmacyService.getAllPrescriptions(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(prescriptions, "Prescriptions retrieved"));
    }

    @GetMapping("/prescriptions/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<PrescriptionResponse>>> getPrescriptionsByPatient(
            Authentication authentication,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        @SuppressWarnings("unused")
        User user = getUser(authentication);
        Page<PrescriptionResponse> prescriptions = pharmacyService.getPrescriptionsByPatient(patientId,
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(prescriptions, "Prescriptions retrieved for patient"));
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
