package com.medsyncpro.controller;

import com.medsyncpro.dto.request.SignatureRequestDTO;
import com.medsyncpro.dto.response.SignatureResponseDTO;
import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Role;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DocumentTypeRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Generic document upload controller — works for ALL roles
 * (DOCTOR, PATIENT, PHARMACY, AGENT) via a single endpoint.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ Signed-upload flow (browser → Cloudinary directly) │
 * │ │
 * │ 1. Browser POST /api/upload/signature │
 * │ { "documentTypeId": "…" } │
 * │ │
 * │ 2. Server validates documentTypeId belongs to the │
 * │ caller's role, returns SignatureResponseDTO │
 * │ │
 * │ 3. Browser POST https://api.cloudinary.com/…/upload │
 * │ (file never touches this server) │
 * │ │
 * │ 4. Browser calls the role-specific save endpoint │
 * │ e.g. POST /api/doctor/documents/upload │
 * │ with the Cloudinary response metadata │
 * └─────────────────────────────────────────────────────────┘
 *
 * Why a shared controller?
 * The signature logic is identical for every role — only the
 * validation (role check) and the Cloudinary folder differ.
 * Role-specific save / submit-verification endpoints remain in
 * their own controllers (DoctorController, PatientController, …)
 * because the business logic there is role-specific.
 *
 * Frontend usage (Next.js server action):
 * Change the action to call POST /api/upload/signature
 * instead of /api/{role}/documents/signature if you want a
 * single shared action. The role-specific endpoints in each
 * controller (e.g. DoctorController) are kept as aliases for
 * backward compatibility.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadController {

    private final FileStorageService fileStorageService;
    private final DocumentTypeRepository documentTypeRepository;

    /**
     * Generate a Cloudinary upload signature for the authenticated user.
     *
     * <p>
     * The endpoint is accessible to any authenticated user regardless of
     * role. The document type is validated against the caller's actual role
     * so a PATIENT cannot obtain a signature for a DOCTOR-only document type.
     */
    @PostMapping("/signature")
    public ResponseEntity<ApiResponse<SignatureResponseDTO>> getUploadSignature(
            Authentication authentication,
            @RequestBody SignatureRequestDTO request) {
        try {
            // ── Derive role from Spring Security authorities ──────────────
            Role callerRole = resolveRole(authentication);

            // ── Validate document type ────────────────────────────────────
            UUID docTypeUuid;
            try {
                docTypeUuid = UUID.fromString(request.getDocumentTypeId());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid documentTypeId format"));
            }

            DocumentType docType = documentTypeRepository.findById(docTypeUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Document type not found"));

            if (!docType.isActive()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Document type is not active"));
            }
            if (docType.getRole() != callerRole) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Document type does not belong to role: " + callerRole));
            }

            // ── Build a per-user, per-role folder ─────────────────────────
            // e.g. medsyncpro/documents/doctors/<userId>
            // medsyncpro/documents/patients/<userId>
            String folderSegment = callerRole.name().toLowerCase() + "s"; // DOCTOR → doctors
            String folder = "medsyncpro/documents/" + folderSegment
                    + "/" + authentication.getName(); // use email as folder name; swap for userId if preferred

            log.info("Generating upload signature — role={} docType={} folder={}",
                    callerRole, docType.getCode(), folder);

            SignatureResponseDTO signature = fileStorageService.generateUploadSignature(folder);
            return ResponseEntity.ok(ApiResponse.success(signature, "Upload signature generated"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Signature generation failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate upload signature"));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Maps the first ROLE_* authority found in the authentication to a
     * {@link Role} enum value.
     *
     * <p>
     * Spring Security prefixes roles with "ROLE_", so "ROLE_DOCTOR" → DOCTOR.
     */
    private Role resolveRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5)) // strip "ROLE_" prefix
                .map(roleName -> {
                    try {
                        return Role.valueOf(roleName);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(r -> r != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No recognisable role found in token"));
    }
}