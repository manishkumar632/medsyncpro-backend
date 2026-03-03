package com.medsyncpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.medsyncpro.dto.doctor.*;
import com.medsyncpro.entity.Doctor;
import com.medsyncpro.entity.DoctorClinic;
import com.medsyncpro.entity.DoctorSettings;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DoctorClinicRepository;
import com.medsyncpro.repository.DoctorRepository;
import com.medsyncpro.repository.DoctorSettingsRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorSettingsService {

    private final DoctorSettingsRepository settingsRepo;
    private final DoctorClinicRepository clinicRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private final DoctorRepository doctorRepository;

    // ── Helper: get-or-create settings row ──
    private DoctorSettings getOrCreateSettings(UUID userId) {
        return settingsRepo.findByUserId(userId)
                .orElseGet(() -> {
                    DoctorSettings ds = new DoctorSettings();
                    ds.setUser(userRepo.getReferenceById(userId));
                    return settingsRepo.save(ds);
                });
    }

    private User findUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ── JSON helpers ──
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJsonList(List<String> list) {
        if (list == null) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Boolean> parseJsonBoolMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ═══════════════════════════════════════════
    // 1. PROFESSIONAL INFO
    // ═══════════════════════════════════════════

    public ProfessionalInfoResponse getProfessionalInfo(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        return ProfessionalInfoResponse.builder()
                .specialty(ds.getSpecialty())
                .qualifications(ds.getQualifications())
                .experienceYears(doctor.getExperienceYears())
                .medRegNumber(ds.getMedRegNumber())
                .consultationFee(ds.getConsultationFee())
                .languages(parseJsonList(ds.getLanguages()))
                .expertise(parseJsonList(ds.getExpertise()))
                .build();
    }

    @Transactional
    public ProfessionalInfoResponse updateProfessionalInfo(UUID userId, ProfessionalInfoRequest req) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        if (req.getSpecialty() != null) ds.setSpecialty(req.getSpecialty());
        if (req.getQualifications() != null) ds.setQualifications(req.getQualifications());
        if (req.getMedRegNumber() != null) ds.setMedRegNumber(req.getMedRegNumber());
        if (req.getConsultationFee() != null) ds.setConsultationFee(req.getConsultationFee());
        if (req.getLanguages() != null) ds.setLanguages(toJsonList(req.getLanguages()));
        if (req.getExpertise() != null) ds.setExpertise(toJsonList(req.getExpertise()));
        if (req.getExperienceYears() != null) doctor.setExperienceYears(req.getExperienceYears());

        ds.setUpdatedAt(LocalDateTime.now());
        doctor.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        doctorRepository.save(doctor);

        return getProfessionalInfo(userId);
    }

    // ═══════════════════════════════════════════
    // 2. CLINICS
    // ═══════════════════════════════════════════

    public List<ClinicResponse> getClinics(UUID userId) {
        return clinicRepo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId)
                .stream()
                .map(c -> ClinicResponse.builder()
                        .id(c.getId().toString())
                        .clinicName(c.getClinicName())
                        .address(c.getAddress())
                        .city(c.getCity())
                        .isPrimary(c.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ClinicResponse addClinic(UUID userId, ClinicRequest req) {
        DoctorClinic clinic = new DoctorClinic();
        clinic.setUser(userRepo.getReferenceById(userId));
        clinic.setClinicName(req.getClinicName());
        clinic.setAddress(req.getAddress());
        clinic.setCity(req.getCity());
        clinic.setIsPrimary(req.getIsPrimary() != null && req.getIsPrimary());

        // If marking as primary, unset others
        if (Boolean.TRUE.equals(clinic.getIsPrimary())) {
            clinicRepo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(userId)
                    .forEach(c -> { c.setIsPrimary(false); clinicRepo.save(c); });
        }

        clinic = clinicRepo.save(clinic);
        return ClinicResponse.builder()
                .id(clinic.getId().toString())
                .clinicName(clinic.getClinicName())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .isPrimary(clinic.getIsPrimary())
                .build();
    }

    @Transactional
    public ClinicResponse updateClinic(String userId, String clinicId, ClinicRequest req) {
        DoctorClinic clinic = clinicRepo.findById(UUID.fromString(clinicId))
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
        if (!clinic.getUser().getId().equals(UUID.fromString(userId))) {
            throw new BusinessException("FORBIDDEN", "Not your clinic");
        }

        if (req.getClinicName() != null) clinic.setClinicName(req.getClinicName());
        if (req.getAddress() != null) clinic.setAddress(req.getAddress());
        if (req.getCity() != null) clinic.setCity(req.getCity());
        if (req.getIsPrimary() != null) {
            if (Boolean.TRUE.equals(req.getIsPrimary())) {
                clinicRepo.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(UUID.fromString(userId))
                        .forEach(c -> { c.setIsPrimary(false); clinicRepo.save(c); });
            }
            clinic.setIsPrimary(req.getIsPrimary());
        }

        clinic = clinicRepo.save(clinic);
        return ClinicResponse.builder()
                .id(clinic.getId().toString())
                .clinicName(clinic.getClinicName())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .isPrimary(clinic.getIsPrimary())
                .build();
    }

    @Transactional
    public void deleteClinic(String userId, String clinicId) {
        DoctorClinic clinic = clinicRepo.findById(UUID.fromString(clinicId))
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
        if (!clinic.getUser().getId().equals(UUID.fromString(userId))) {
            throw new BusinessException("FORBIDDEN", "Not your clinic");
        }
        clinicRepo.delete(clinic);
    }

    // ═══════════════════════════════════════════
    // 3. AVAILABILITY
    // ═══════════════════════════════════════════

    public Map<String, Object> getAvailability(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableForConsultation", ds.getAvailableForConsultation());

        if (ds.getWeeklySchedule() != null && !ds.getWeeklySchedule().isBlank()) {
            try {
                result.put("weeklySchedule", objectMapper.readValue(ds.getWeeklySchedule(), Object.class));
            } catch (Exception e) {
                result.put("weeklySchedule", getDefaultSchedule());
            }
        } else {
            result.put("weeklySchedule", getDefaultSchedule());
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateAvailability(UUID userId, AvailabilityRequest req) {
        DoctorSettings ds = getOrCreateSettings(userId);
        if (req.getAvailableForConsultation() != null) ds.setAvailableForConsultation(req.getAvailableForConsultation());
        if (req.getWeeklySchedule() != null) ds.setWeeklySchedule(toJson(req.getWeeklySchedule()));
        ds.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        return getAvailability(userId);
    }

    private Map<String, Object> getDefaultSchedule() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        Map<String, Object> schedule = new LinkedHashMap<>();
        for (String day : days) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            if ("Sunday".equals(day)) {
                dayData.put("enabled", false);
                dayData.put("slots", List.of());
            } else {
                dayData.put("enabled", true);
                dayData.put("slots", List.of(
                    Map.of("start", "09:00", "end", "13:00"),
                    Map.of("start", "14:00", "end", "18:00")
                ));
            }
            schedule.put(day, dayData);
        }
        return schedule;
    }

    // ═══════════════════════════════════════════
    // 4. CONSULTATION SETTINGS
    // ═══════════════════════════════════════════

    public Map<String, Object> getConsultationSettings(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("slotDurationMinutes", ds.getSlotDurationMinutes());
        result.put("followUpWindowDays", ds.getFollowUpWindowDays());
        result.put("prescriptionTemplate", ds.getPrescriptionTemplate());
        result.put("autoApproveAppointments", ds.getAutoApproveAppointments());
        result.put("onlineConsultationEnabled", ds.getOnlineConsultationEnabled());
        return result;
    }

    @Transactional
    public Map<String, Object> updateConsultationSettings(UUID userId, ConsultationSettingsRequest req) {
        DoctorSettings ds = getOrCreateSettings(userId);
        if (req.getSlotDurationMinutes() != null) ds.setSlotDurationMinutes(req.getSlotDurationMinutes());
        if (req.getFollowUpWindowDays() != null) ds.setFollowUpWindowDays(req.getFollowUpWindowDays());
        if (req.getPrescriptionTemplate() != null) ds.setPrescriptionTemplate(req.getPrescriptionTemplate());
        if (req.getAutoApproveAppointments() != null) ds.setAutoApproveAppointments(req.getAutoApproveAppointments());
        if (req.getOnlineConsultationEnabled() != null) ds.setOnlineConsultationEnabled(req.getOnlineConsultationEnabled());
        ds.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        return getConsultationSettings(userId);
    }

    // ═══════════════════════════════════════════
    // 5. NOTIFICATION PREFERENCES
    // ═══════════════════════════════════════════

    private static final Map<String, Boolean> DEFAULT_NOTIF_PREFS = Map.of(
            "newAppointment", true,
            "messages", true,
            "prescriptionReminders", true,
            "followUpReminders", true,
            "systemUpdates", false,
            "emailNotifs", true,
            "pushNotifs", true
    );

    public Map<String, Boolean> getNotificationPrefs(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Boolean> stored = parseJsonBoolMap(ds.getNotificationPrefs());
        if (stored.isEmpty()) return new LinkedHashMap<>(DEFAULT_NOTIF_PREFS);
        // Merge with defaults (in case new keys added)
        Map<String, Boolean> merged = new LinkedHashMap<>(DEFAULT_NOTIF_PREFS);
        merged.putAll(stored);
        return merged;
    }

    @Transactional
    public Map<String, Boolean> updateNotificationPrefs(UUID userId, NotificationPrefsRequest req) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Boolean> current = getNotificationPrefs(userId);
        if (req.getPrefs() != null) current.putAll(req.getPrefs());
        ds.setNotificationPrefs(toJson(current));
        ds.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        return current;
    }

    // ═══════════════════════════════════════════
    // 6. PRIVACY SETTINGS
    // ═══════════════════════════════════════════

    private static final Map<String, Boolean> DEFAULT_PRIVACY = Map.of(
            "profileVisible", true,
            "allowReviews", true,
            "showContact", false,
            "dataSharing", false
    );

    public Map<String, Boolean> getPrivacySettings(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Boolean> stored = parseJsonBoolMap(ds.getPrivacySettings());
        if (stored.isEmpty()) return new LinkedHashMap<>(DEFAULT_PRIVACY);
        Map<String, Boolean> merged = new LinkedHashMap<>(DEFAULT_PRIVACY);
        merged.putAll(stored);
        return merged;
    }

    @Transactional
    public Map<String, Boolean> updatePrivacySettings(UUID userId, PrivacySettingsRequest req) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Boolean> current = getPrivacySettings(userId);
        if (req.getSettings() != null) current.putAll(req.getSettings());
        ds.setPrivacySettings(toJson(current));
        ds.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        return current;
    }

    // ═══════════════════════════════════════════
    // 7. SECURITY
    // ═══════════════════════════════════════════

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "New password and confirm password do not match");
        }

        User user = findUser(userId);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("WRONG_PASSWORD", "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1); // Invalidate existing sessions
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        log.info("Password changed for user {}", userId);
    }

    @Transactional
    public Map<String, Boolean> toggleTwoFactor(UUID userId, boolean enabled) {
        DoctorSettings ds = getOrCreateSettings(userId);
        ds.setTwoFactorEnabled(enabled);
        ds.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(ds);
        return Map.of("twoFactorEnabled", enabled);
    }

    public Map<String, Object> getSecurityInfo(UUID userId) {
        DoctorSettings ds = getOrCreateSettings(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("twoFactorEnabled", ds.getTwoFactorEnabled());
        return result;
    }

    // ═══════════════════════════════════════════
    // 8. ACCOUNT
    // ═══════════════════════════════════════════

    public AccountSummaryResponse getAccountSummary(UUID userId) {
        User user = findUser(userId);
        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(() -> new BusinessException("DOCTOR_PROFILE_NOT_FOUND", "Doctor profile not found for user ID: " + userId));
        return AccountSummaryResponse.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .name(doctor.getName())
                .role(user.getRole().name())
                .verificationStatus(doctor.isVerified() ? "VERIFIED" : "PENDING")
                .emailVerified(user.isEmailVerified())
                .isActive(!user.getDeleted())
                .memberSince(user.getCreatedAt())
                .lastUpdated(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = findUser(userId);
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        log.info("Account deactivated for user {}", userId);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findUser(userId);
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
        log.info("Account deletion requested for user {}", userId);
    }
}
