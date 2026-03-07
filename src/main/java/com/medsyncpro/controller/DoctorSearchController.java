package com.medsyncpro.controller;

import com.medsyncpro.dto.doctor.DoctorPublicProfile;
import com.medsyncpro.dto.doctor.DoctorSearchResult;
import com.medsyncpro.dto.response.SlotResponse;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.DoctorSearchService;
import com.medsyncpro.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorSearchController {

    private final DoctorSearchService doctorSearchService;
    private final PatientService patientService;

    // ──────────────────────────────────────────────────────────────
    // GET /api/doctors/search
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<DoctorSearchResult>>> searchDoctors(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        int safeSize = Math.min(size, 50);

        Sort.Direction sortDir = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(sortDir, sort));
        Page<DoctorSearchResult> results = doctorSearchService.searchDoctors(q, pageable);

        String message = results.isEmpty()
                ? "No doctors found matching your search"
                : "Found " + results.getTotalElements() + " doctor(s)";

        return ResponseEntity.ok(ApiResponse.success(results, message));
    }

    // ──────────────────────────────────────────────────────────────
    // GET /api/doctors/{id}
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorPublicProfile>> getDoctorProfile(
            @PathVariable String id) {

        DoctorPublicProfile profile = doctorSearchService.getDoctorProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile, "Doctor profile retrieved successfully"));
    }

    // ──────────────────────────────────────────────────────────────
    // GET /api/doctors/{id}/slots
    //
    // Generate available appointment slots for a doctor based on
    // their weekly schedule and existing bookings.
    //
    // Query params:
    // type – consultation type filter (VIDEO, IN_PERSON, CHAT)
    //
    // Access: PUBLIC (authenticated patients use this)
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "VIDEO") String type) {

        List<SlotResponse> slots = patientService.getAvailableSlots(UUID.fromString(id), type);
        return ResponseEntity.ok(ApiResponse.success(slots, "Available slots retrieved"));
    }
}
