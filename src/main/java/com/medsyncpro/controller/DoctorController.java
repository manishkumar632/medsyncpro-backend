package com.medsyncpro.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.medsyncpro.dto.request.DoctorDocumentUploadRequest;
import com.medsyncpro.dto.request.AppointmentRescheduleRequest;
import com.medsyncpro.dto.request.SignatureRequestDTO;
import com.medsyncpro.dto.response.AppointmentResponse;
import com.medsyncpro.dto.response.DoctorProfileResponseDTO;
import com.medsyncpro.dto.response.RequiredDocumentItem;
import com.medsyncpro.dto.response.SignatureResponseDTO;
import com.medsyncpro.dto.response.VerificationStatusResponse;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.DoctorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final UserRepository userRepository;
    private final DoctorService doctorService;

    // ─── Profile ─────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileResponseDTO>> getDoctorProfileData(
            Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(doctorService.getDoctorProfileData(user.getId()));
    }

    // ─── Appointments ─────────────────────────────────────────────────────────

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getAppointments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        User user = getUser(authentication);
        Page<AppointmentResponse> appointments = doctorService.getDoctorAppointments(
                user.getId(), PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(appointments, "Appointments retrieved"));
    }


    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyPatients(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getUser(authentication);
        Page<Map<String, Object>> patients = doctorService.getDoctorPatients(
                user.getId(), search, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(patients, "Patients retrieved successfully"));
    }

    @PatchMapping("/appointments/{id}/approve")
    public ResponseEntity<ApiResponse<AppointmentResponse>> approveAppointment(
            Authentication authentication, @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.approveAppointment(getUser(authentication).getId(), id),
                    "Appointment approved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/appointments/{id}/reject")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rejectAppointment(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.rejectAppointment(getUser(authentication).getId(), id, reason),
                    "Appointment rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/appointments/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(
            Authentication authentication, @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.completeAppointment(getUser(authentication).getId(), id),
                    "Appointment completed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/appointments/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.cancelAppointment(getUser(authentication).getId(), id, reason),
                    "Appointment cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/appointments/{id}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody AppointmentRescheduleRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.rescheduleAppointment(getUser(authentication).getId(), id, request),
                    "Appointment rescheduled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/appointments/{id}/notes")
    public ResponseEntity<ApiResponse<AppointmentResponse>> saveNotes(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.saveNotes(getUser(authentication).getId(), id, body),
                    "Notes saved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Paste this inside your DoctorController class
    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMyPrescriptions(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getUser(authentication);
        Page<Map<String, Object>> prescriptions = doctorService.getDoctorPrescriptions(
                user.getId(), search, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(prescriptions, "Prescriptions retrieved successfully"));
    }


    @GetMapping("/patients/{patientId}/appointments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPatientAppointments(
            Authentication authentication,
            @PathVariable UUID patientId) {
        User user = getUser(authentication);
        List<Map<String, Object>> appointments = doctorService.getPatientAppointmentsForDoctor(user.getId(), patientId);
        return ResponseEntity.ok(ApiResponse.success(appointments,
                "Patient appointments retrieved successfully"));
    }

    

    @PostMapping("/appointments/{id}/prescription")
    public ResponseEntity<ApiResponse<AppointmentResponse>> savePrescription(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    doctorService.savePrescription(getUser(authentication).getId(), id, body),
                    "Prescription saved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Document Types ───────────────────────────────────────────────────────

    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<DocumentType>>> getDocumentTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                doctorService.getActiveDocumentTypes(),
                "Active document types for DOCTOR"));
    }

    // ─── Verification Status ──────────────────────────────────────────────────

    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getVerificationStatus(
            Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                doctorService.getVerificationStatus(user.getId()),
                "Verification status retrieved"));
    }

    // ─── Cloudinary Upload Signature ──────────────────────────────────────────
    //
    // Generates a time-limited signature so the browser can POST the file
    // directly to Cloudinary (no file bytes pass through this server).
    //
    // Flow:
    // 1. Browser → POST /api/doctor/documents/signature { documentTypeId }
    // 2. Server → validates docType belongs to DOCTOR role, returns
    // SignatureResponseDTO
    // 3. Browser → POST https://api.cloudinary.com/v1_1/{cloud}/auto/upload
    // 4. Browser → POST /api/doctor/documents/upload { publicId, secureUrl, … }

    @PostMapping("/documents/signature")
    public ResponseEntity<ApiResponse<SignatureResponseDTO>> getUploadSignature(
            Authentication authentication,
            @RequestBody SignatureRequestDTO request) {
        try {
            User user = getUser(authentication);
            SignatureResponseDTO signature = doctorService.generateDocumentUploadSignature(
                    user.getId(), request.getDocumentTypeId());
            return ResponseEntity.ok(ApiResponse.success(signature, "Upload signature generated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Save Uploaded Document ───────────────────────────────────────────────

    @PostMapping("/documents/upload")
    public ResponseEntity<ApiResponse<RequiredDocumentItem>> saveUploadedDocument(
            Authentication authentication,
            @RequestBody DoctorDocumentUploadRequest request) {
        try {
            User user = getUser(authentication);
            RequiredDocumentItem result = doctorService.saveUploadedDocument(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success(result, "Document saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Submit for Verification ──────────────────────────────────────────────

    @PostMapping("/documents/submit-verification")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> submitForVerification(
            Authentication authentication) {
        try {
            User user = getUser(authentication);
            VerificationStatusResponse response = doctorService.submitForVerification(user.getId());
            return ResponseEntity.ok(ApiResponse.success(response, "Verification submitted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
