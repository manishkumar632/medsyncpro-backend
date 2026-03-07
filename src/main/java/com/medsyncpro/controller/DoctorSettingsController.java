package com.medsyncpro.controller;

import com.medsyncpro.dto.doctor.*;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.DoctorSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.medsyncpro.utils.UserProfileHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/settings")
@RequiredArgsConstructor
public class DoctorSettingsController {

    private final DoctorSettingsService service;
    private final UserRepository userRepository;
    private final UserProfileHelper userProfileHelper;

    // JWT sets auth.getName() = email, not userId. Resolve it.
    private UUID getUserId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user == null) throw new ResourceNotFoundException("User not found");
        return user.getId();
    }

    // ═══════════════════════════════
    // UNIFIED GET — all settings in one call
    // ═══════════════════════════════

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllSettings(Authentication auth) {
        UUID userId = getUserId(auth);
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("professional", service.getProfessionalInfo(userId));
        all.put("clinics", service.getClinics(userId));
        all.put("availability", service.getAvailability(userId));
        all.put("consultation", service.getConsultationSettings(userId));
        all.put("notifications", service.getNotificationPrefs(userId));
        all.put("privacy", service.getPrivacySettings(userId));
        all.put("security", service.getSecurityInfo(userId));
        all.put("account", service.getAccountSummary(userId));
        return ResponseEntity.ok(ApiResponse.success(all, "All settings retrieved"));
    }

    // ═══════════════════════════════
    // PROFESSIONAL INFO
    // ═══════════════════════════════

    @GetMapping("/professional")
    public ResponseEntity<ApiResponse<ProfessionalInfoResponse>> getProfessionalInfo(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getProfessionalInfo(getUserId(auth)), "Professional info retrieved"));
    }

    @PutMapping("/professional")
    public ResponseEntity<ApiResponse<ProfessionalInfoResponse>> updateProfessionalInfo(
            Authentication auth, @RequestBody ProfessionalInfoRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateProfessionalInfo(getUserId(auth), req), "Professional info updated"));
    }

    // ═══════════════════════════════
    // CLINICS
    // ═══════════════════════════════

    @GetMapping("/clinics")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> getClinics(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getClinics(getUserId(auth)), "Clinics retrieved"));
    }

    @PostMapping("/clinics")
    public ResponseEntity<ApiResponse<ClinicResponse>> addClinic(
            Authentication auth, @RequestBody ClinicRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.addClinic(getUserId(auth), req), "Clinic added"));
    }

    @PutMapping("/clinics/{id}")
    public ResponseEntity<ApiResponse<ClinicResponse>> updateClinic(
            Authentication auth, @PathVariable String id, @RequestBody ClinicRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateClinic(String.valueOf(getUserId(auth)), id, req), "Clinic updated"));
    }

    @DeleteMapping("/clinics/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClinic(
            Authentication auth, @PathVariable String id) {
        service.deleteClinic(String.valueOf(getUserId(auth)), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Clinic deleted"));
    }

    // ═══════════════════════════════
    // AVAILABILITY
    // ═══════════════════════════════

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailability(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAvailability(getUserId(auth)), "Availability retrieved"));
    }

    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAvailability(
            Authentication auth, @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateAvailability(getUserId(auth), req), "Availability updated"));
    }

    // ═══════════════════════════════
    // CONSULTATION SETTINGS
    // ═══════════════════════════════

    @GetMapping("/consultation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConsultation(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getConsultationSettings(getUserId(auth)), "Consultation settings retrieved"));
    }

    @PutMapping("/consultation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConsultation(
            Authentication auth, @RequestBody ConsultationSettingsRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateConsultationSettings(getUserId(auth), req), "Consultation settings updated"));
    }

    // ═══════════════════════════════
    // NOTIFICATION PREFERENCES
    // ═══════════════════════════════

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getNotifPrefs(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getNotificationPrefs(getUserId(auth)), "Notification preferences retrieved"));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> updateNotifPrefs(
            Authentication auth, @RequestBody NotificationPrefsRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateNotificationPrefs(getUserId(auth), req), "Notification preferences updated"));
    }

    // ═══════════════════════════════
    // PRIVACY SETTINGS
    // ═══════════════════════════════

    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPrivacy(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getPrivacySettings(getUserId(auth)), "Privacy settings retrieved"));
    }

    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> updatePrivacy(
            Authentication auth, @RequestBody PrivacySettingsRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updatePrivacySettings(getUserId(auth), req), "Privacy settings updated"));
    }

    // ═══════════════════════════════
    // SECURITY
    // ═══════════════════════════════

    @GetMapping("/security")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityInfo(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getSecurityInfo(getUserId(auth)), "Security info retrieved"));
    }

    @PostMapping("/security/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(getUserId(auth), req);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PutMapping("/security/two-factor")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleTwoFactor(
            Authentication auth, @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseEntity.ok(ApiResponse.success(
                service.toggleTwoFactor(getUserId(auth), enabled), "Two-factor setting updated"));
    }

    // ═══════════════════════════════
    // ACCOUNT
    // ═══════════════════════════════

    @GetMapping("/account/summary")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getAccountSummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAccountSummary(getUserId(auth)), "Account summary retrieved"));
    }

    @PostMapping("/account/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication auth) {
        service.deactivateAccount(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated"));
    }

    @PostMapping("/account/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication auth) {
        service.deleteAccount(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success(null, "Account deletion requested"));
    }
}
