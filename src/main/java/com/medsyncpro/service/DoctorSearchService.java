package com.medsyncpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.medsyncpro.dto.doctor.DoctorPublicProfile;
import com.medsyncpro.dto.doctor.DoctorSearchResult;
import com.medsyncpro.entity.DoctorClinic;
import com.medsyncpro.dto.doctor.ClinicResponse;
import com.medsyncpro.entity.DoctorSettings;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationStatus;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.DoctorClinicRepository;
import com.medsyncpro.repository.DoctorSettingsRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorSearchService {

    private final UserRepository userRepository;
    private final DoctorSettingsRepository doctorSettingsRepository;
    private final DoctorClinicRepository doctorClinicRepository;

    // Instantiated directly — avoids Spring bean injection ambiguity.
    // ObjectMapper is not auto-registered as a bean in all Spring Boot configs.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ══════════════════════════════════════════════════════════════
    // SEARCH — multi-field, paginated
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<DoctorSearchResult> searchDoctors(String query, Pageable pageable) {

        Page<User> doctorPage = (query == null || query.isBlank())
                ? userRepository.findByRoleAndVerificationStatus(
                        Role.DOCTOR, VerificationStatus.VERIFIED, pageable)
                : userRepository.findVerifiedDoctorsBySearch(
                        Role.DOCTOR, VerificationStatus.VERIFIED, query.trim(), pageable);

        List<User> doctors = doctorPage.getContent();

        if (doctors.isEmpty()) {
            return Page.empty(pageable);
        }

        List<String> doctorIds = doctors.stream().map(User::getId).collect(Collectors.toList());
        Map<String, DoctorSettings> settingsMap = buildSettingsMap(doctorIds);
        Map<String, List<DoctorClinic>> clinicsMap = buildClinicsMap(doctorIds);

        List<DoctorSearchResult> results = doctors.stream()
                .filter(u -> !u.getDeleted())
                .filter(u -> isProfileVisible(settingsMap.get(u.getId())))
                .filter(u -> matchesQuery(u, settingsMap.get(u.getId()),
                        clinicsMap.get(u.getId()), query))
                .map(u -> toSearchResult(u,
                        settingsMap.get(u.getId()),
                        clinicsMap.getOrDefault(u.getId(), List.of())))
                .collect(Collectors.toList());

        return new PageImpl<>(results, pageable, doctorPage.getTotalElements());
    }

    // ══════════════════════════════════════════════════════════════
    // PUBLIC PROFILE — full detail for one doctor
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DoctorPublicProfile getDoctorProfile(String doctorId) {

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (doctor.getDeleted()) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        if (doctor.getRole() != Role.DOCTOR) {
            throw new ResourceNotFoundException("Doctor not found");
        }

        DoctorSettings settings = doctorSettingsRepository
                .findByUserId(doctorId)
                .orElse(new DoctorSettings());

        if (!isProfileVisible(settings)) {
            throw new ResourceNotFoundException("This doctor's profile is not publicly visible");
        }

        List<DoctorClinic> clinics = doctorClinicRepository
                .findByUserIdOrderByIsPrimaryDescCreatedAtAsc(doctorId);

        boolean showContact = getPrivacyFlag(settings, "showContact", false);
        Object weeklySchedule = parseWeeklySchedule(settings.getWeeklySchedule());

        return DoctorPublicProfile.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .email(showContact ? doctor.getEmail() : null)
                .phone(showContact ? doctor.getPhone() : null)
                .profileImageUrl(doctor.getProfileImageUrl())
                .bio(doctor.getBio())
                .gender(doctor.getGender() != null ? doctor.getGender().name() : null)
                .city(doctor.getCity())
                .state(doctor.getState())
                .verificationStatus(doctor.getProfessionalVerificationStatus().name())
                .specialty(settings.getSpecialty())
                .qualifications(settings.getQualifications())
                .experienceYears(doctor.getExperienceYears())
                .medRegNumber(settings.getMedRegNumber())
                .consultationFee(settings.getConsultationFee())
                .languages(parseJsonList(settings.getLanguages()))
                .expertise(parseJsonList(settings.getExpertise()))
                .clinics(clinics.stream()
                        .map(c -> ClinicResponse.builder()
                                .id(c.getId())
                                .clinicName(c.getClinicName())
                                .address(c.getAddress())
                                .city(c.getCity())
                                .isPrimary(c.getIsPrimary())
                                .build())
                        .collect(Collectors.toList()))
                .availableForConsultation(settings.getAvailableForConsultation())
                .onlineConsultationEnabled(settings.getOnlineConsultationEnabled())
                .weeklySchedule(weeklySchedule)
                .slotDurationMinutes(settings.getSlotDurationMinutes())
                .autoApproveAppointments(settings.getAutoApproveAppointments())
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    private Map<String, DoctorSettings> buildSettingsMap(List<String> doctorIds) {
        Map<String, DoctorSettings> map = new HashMap<>();
        for (String id : doctorIds) {
            doctorSettingsRepository.findByUserId(id).ifPresent(s -> map.put(id, s));
        }
        return map;
    }

    private Map<String, List<DoctorClinic>> buildClinicsMap(List<String> doctorIds) {
        Map<String, List<DoctorClinic>> map = new HashMap<>();
        for (String id : doctorIds) {
            map.put(id, doctorClinicRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(id));
        }
        return map;
    }

    private boolean isProfileVisible(DoctorSettings settings) {
        return getPrivacyFlag(settings, "profileVisible", true);
    }

    private boolean getPrivacyFlag(DoctorSettings settings, String key, boolean defaultValue) {
        if (settings == null || settings.getPrivacySettings() == null) {
            return defaultValue;
        }
        try {
            Map<String, Boolean> privacy = objectMapper.readValue(
                    settings.getPrivacySettings(), new TypeReference<Map<String, Boolean>>() {});
            return privacy.getOrDefault(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean matchesQuery(User user, DoctorSettings settings,
            List<DoctorClinic> clinics, String query) {
        if (query == null || query.isBlank()) return true;

        String q = query.toLowerCase().trim();

        if (settings != null) {
            if (settings.getSpecialty() != null
                    && settings.getSpecialty().toLowerCase().contains(q)) return true;
            if (settings.getQualifications() != null
                    && settings.getQualifications().toLowerCase().contains(q)) return true;
        }
        if (clinics != null) {
            for (DoctorClinic clinic : clinics) {
                if (clinic.getClinicName() != null
                        && clinic.getClinicName().toLowerCase().contains(q)) return true;
                if (clinic.getCity() != null
                        && clinic.getCity().toLowerCase().contains(q)) return true;
            }
        }
        return false;
    }

    private DoctorSearchResult toSearchResult(User user, DoctorSettings settings,
            List<DoctorClinic> clinics) {
        DoctorClinic primaryClinic = clinics.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPrimary()))
                .findFirst()
                .orElse(clinics.isEmpty() ? null : clinics.get(0));

        return DoctorSearchResult.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .specialty(settings != null ? settings.getSpecialty() : null)
                .qualifications(settings != null ? settings.getQualifications() : null)
                .experienceYears(user.getExperienceYears())
                .consultationFee(settings != null ? settings.getConsultationFee() : null)
                .languages(parseJsonList(settings != null ? settings.getLanguages() : null))
                .expertise(parseJsonList(settings != null ? settings.getExpertise() : null))
                .verificationStatus(user.getProfessionalVerificationStatus().name())
                .city(user.getCity())
                .state(user.getState())
                .primaryClinicName(primaryClinic != null ? primaryClinic.getClinicName() : null)
                .primaryClinicCity(primaryClinic != null ? primaryClinic.getCity() : null)
                .availableForConsultation(settings != null ? settings.getAvailableForConsultation() : null)
                .onlineConsultationEnabled(settings != null ? settings.getOnlineConsultationEnabled() : null)
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Object parseWeeklySchedule(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}