package com.medsyncpro.controller;

import com.medsyncpro.dto.doctor.DoctorPublicProfile;
import com.medsyncpro.dto.doctor.DoctorSearchResult;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.DoctorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorSearchController {

    private final DoctorSearchService doctorSearchService;

    // ──────────────────────────────────────────────────────────────
    // GET /api/doctors/search
    //
    // Search for doctors across multiple fields.
    //
    // Query params:
    // q – search term (name / email / phone / specialty / clinic / city)
    // leave blank to get all verified doctors
    // page – 0-indexed page number (default: 0)
    // size – page size (default: 10, max: 50)
    // sort – field to sort by (default: name)
    // direction– asc | desc (default: asc)
    //
    // Access: PUBLIC (no authentication required)
    //
    // Example:
    // GET /api/doctors/search?q=cardio&page=0&size=10
    // GET /api/doctors/search?q=Dr+Smith&sort=consultationFee&direction=asc
    // GET /api/doctors/search ← returns all verified doctors, page 0
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<DoctorSearchResult>>> searchDoctors(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        // Cap page size to prevent abuse
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
    //
    // Fetch the full public profile of a single doctor.
    // Returns 404 if the doctor doesn't exist, is deleted, or has
    // set their profile to private.
    //
    // Access: PUBLIC (no authentication required)
    //
    // Example:
    // GET /api/doctors/a1b2c3d4-e5f6-7890-abcd-ef1234567890
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorPublicProfile>> getDoctorProfile(
            @PathVariable String id) {

        DoctorPublicProfile profile = doctorSearchService.getDoctorProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile, "Doctor profile retrieved successfully"));
    }
}