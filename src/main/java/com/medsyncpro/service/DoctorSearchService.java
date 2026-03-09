package com.medsyncpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsyncpro.dto.doctor.*;
import com.medsyncpro.entity.*;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.*;
import com.medsyncpro.utils.UserProfileHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorSearchService {

    private final UserRepository userRepository;
    private final UserProfileHelper userProfileHelper;
    private final DoctorSettingsRepository doctorSettingsRepository;
    private final DoctorClinicRepository doctorClinicRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ══════════════════════════════════════════════════════════════
    // SEARCH DOCTORS
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<DoctorSearchResult> searchDoctors(
            String query,
            String specialization,
            String location,
            boolean availableOnly,
            Pageable pageable) {

        // 1. Intercept and remove the "name" sort from the DB query
        Pageable dbPageable = pageable;
        boolean sortByName = false;
        Sort.Direction nameDirection = Sort.Direction.ASC;

        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if ("name".equalsIgnoreCase(order.getProperty())) {
                    sortByName = true;
                    nameDirection = order.getDirection();
                    break;
                }
            }
        }

        if (sortByName) {
            // Strip the sort property to prevent Hibernate UnknownPathException
            dbPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        // Fetch all doctors (role-based) using the safe pageable
        Page<User> doctorPage = userRepository.findByRoleAndDeletedFalse(Role.DOCTOR, dbPageable);

        List<User> doctors = doctorPage.getContent();

        if (doctors.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> doctorIds = doctors.stream().map(User::getId).collect(Collectors.toList());

        Map<UUID, DoctorSettings> settingsMap = buildSettingsMap(doctorIds);
        Map<UUID, List<DoctorClinic>> clinicsMap = buildClinicsMap(doctorIds);

        List<DoctorSearchResult> results = doctors.stream()
                // Only VERIFIED doctors
                .filter(u -> userProfileHelper.getVerificationStatus(u) == VerificationStatus.VERIFIED)
                // Only visible profiles
                .filter(u -> isProfileVisible(settingsMap.get(u.getId())))
                // Search filtering
                .filter(u -> matchesQuery(
                        u,
                        settingsMap.get(u.getId()),
                        clinicsMap.get(u.getId()),
                        query))
                .filter(u -> matchesSpecialization(settingsMap.get(u.getId()), specialization))
                .filter(u -> matchesLocation(
                        u,
                        settingsMap.get(u.getId()),
                        clinicsMap.get(u.getId()),
                        location))
                .filter(u -> !availableOnly || Boolean.TRUE.equals(settingsMap.get(u.getId()) != null
                        ? settingsMap.get(u.getId()).getAvailableForConsultation()
                        : false))
                .map(u -> toSearchResult(
                        u,
                        settingsMap.get(u.getId()),
                        clinicsMap.getOrDefault(u.getId(), List.of())))
                .collect(Collectors.toList());

        // 2. Re-apply the alphabetical name sort in-memory
        if (sortByName) {
            Comparator<DoctorSearchResult> comp = Comparator.comparing(
                    d -> d.getName() == null ? "" : d.getName().toLowerCase());
            if (nameDirection == Sort.Direction.DESC) {
                comp = comp.reversed();
            }
            results.sort(comp);
        }

        // Return the modified results while preserving the original frontend pagination
        // state
        return new PageImpl<>(results, pageable, doctorPage.getTotalElements());
    }

    // ══════════════════════════════════════════════════════════════
    // PUBLIC PROFILE
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DoctorPublicProfile getDoctorProfile(String doctorId) {

        User doctor = userRepository.findById(UUID.fromString(doctorId))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (doctor.getDeleted() || doctor.getRole() != Role.DOCTOR) {
            throw new ResourceNotFoundException("Doctor not found");
        }

        if (userProfileHelper.getVerificationStatus(doctor) != VerificationStatus.VERIFIED) {
            throw new ResourceNotFoundException("Doctor not found");
        }

        DoctorSettings settings = doctorSettingsRepository.findByUserId(doctor.getId())
                .orElse(new DoctorSettings());

        if (!isProfileVisible(settings)) {
            throw new ResourceNotFoundException("Profile not visible");
        }

        List<DoctorClinic> clinics = doctorClinicRepository
                .findByUserIdOrderByIsPrimaryDescCreatedAtAsc(doctor.getId());

        boolean showContact = getPrivacyFlag(settings, "showContact", false);

        return DoctorPublicProfile.builder()
                .id(doctor.getId().toString())
                .name(userProfileHelper.getName(doctor))
                .email(showContact ? doctor.getEmail() : null)
                .phone(showContact ? doctor.getPhone() : null)
                .profileImageUrl(userProfileHelper.getProfileImage(doctor))
                .bio(userProfileHelper.getBio(doctor))
                .gender(userProfileHelper.getGender(doctor) != null
                        ? userProfileHelper.getGender(doctor).name()
                        : null)
                .city(userProfileHelper.getCity(doctor))
                .state(userProfileHelper.getState(doctor))
                .verificationStatus(
                        userProfileHelper.getVerificationStatus(doctor).name())
                .specialty(settings.getSpecialty())
                .qualifications(settings.getQualifications())
                .experienceYears(userProfileHelper.getExperienceYears(doctor))
                .consultationFee(settings.getConsultationFee())
                .languages(parseJsonList(settings.getLanguages()))
                .expertise(parseJsonList(settings.getExpertise()))
                .clinics(clinics.stream()
                        .map(c -> ClinicResponse.builder()
                                .id(String.valueOf(c.getId()))
                                .clinicName(c.getClinicName())
                                .address(c.getAddress())
                                .city(c.getCity())
                                .isPrimary(c.getIsPrimary())
                                .build())
                        .collect(Collectors.toList()))
                .availableForConsultation(settings.getAvailableForConsultation())
                .onlineConsultationEnabled(settings.getOnlineConsultationEnabled())
                .slotDurationMinutes(settings.getSlotDurationMinutes())
                .autoApproveAppointments(settings.getAutoApproveAppointments())
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private Map<UUID, DoctorSettings> buildSettingsMap(List<UUID> ids) {
        Map<UUID, DoctorSettings> map = new HashMap<>();
        for (UUID id : ids) {
            doctorSettingsRepository.findByUserId(id)
                    .ifPresent(s -> map.put(id, s));
        }
        return map;
    }

    private Map<UUID, List<DoctorClinic>> buildClinicsMap(List<UUID> ids) {
        Map<UUID, List<DoctorClinic>> map = new HashMap<>();
        for (UUID id : ids) {
            map.put(id,
                    doctorClinicRepository
                            .findByUserIdOrderByIsPrimaryDescCreatedAtAsc(id));
        }
        return map;
    }

    private boolean isProfileVisible(DoctorSettings settings) {
        return getPrivacyFlag(settings, "profileVisible", true);
    }

    private boolean getPrivacyFlag(DoctorSettings settings,
            String key,
            boolean defaultValue) {
        if (settings == null || settings.getPrivacySettings() == null)
            return defaultValue;

        try {
            Map<String, Boolean> privacy = objectMapper.readValue(settings.getPrivacySettings(),
                    new TypeReference<>() {
                    });
            return privacy.getOrDefault(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean matchesQuery(User user,
            DoctorSettings settings,
            List<DoctorClinic> clinics,
            String query) {

        if (query == null || query.isBlank())
            return true;

        String q = query.toLowerCase();

        if (userProfileHelper.getName(user) != null &&
                userProfileHelper.getName(user).toLowerCase().contains(q))
            return true;

        if (settings != null) {
            if (settings.getSpecialty() != null &&
                    settings.getSpecialty().toLowerCase().contains(q))
                return true;
            if (settings.getQualifications() != null &&
                    settings.getQualifications().toLowerCase().contains(q))
                return true;
        }

        if (clinics != null) {
            for (DoctorClinic clinic : clinics) {
                if (clinic.getClinicName() != null &&
                        clinic.getClinicName().toLowerCase().contains(q))
                    return true;
            }
        }

        return false;
    }

    private boolean matchesSpecialization(DoctorSettings settings, String specialization) {
        if (specialization == null || specialization.isBlank()) {
            return true;
        }
        if (settings == null || settings.getSpecialty() == null) {
            return false;
        }
        return settings.getSpecialty().toLowerCase().contains(specialization.toLowerCase());
    }

    private boolean matchesLocation(User user, DoctorSettings settings, List<DoctorClinic> clinics, String location) {
        if (location == null || location.isBlank()) {
            return true;
        }
        String q = location.toLowerCase();

        if (userProfileHelper.getCity(user) != null && userProfileHelper.getCity(user).toLowerCase().contains(q)) {
            return true;
        }
        if (userProfileHelper.getState(user) != null && userProfileHelper.getState(user).toLowerCase().contains(q)) {
            return true;
        }
        if (clinics != null) {
            for (DoctorClinic clinic : clinics) {
                if (clinic.getCity() != null && clinic.getCity().toLowerCase().contains(q)) {
                    return true;
                }
                if (clinic.getAddress() != null && clinic.getAddress().toLowerCase().contains(q)) {
                    return true;
                }
            }
        }
        if (settings != null && settings.getSpecialty() != null && settings.getSpecialty().toLowerCase().contains(q)) {
            return true;
        }

        return false;
    }

    private DoctorSearchResult toSearchResult(User user,
            DoctorSettings settings,
            List<DoctorClinic> clinics) {

        DoctorClinic primary = clinics.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPrimary()))
                .findFirst()
                .orElse(null);

        return DoctorSearchResult.builder()
                .id(user.getId().toString())
                .name(userProfileHelper.getName(user))
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(userProfileHelper.getProfileImage(user))
                .bio(userProfileHelper.getBio(user))
                .specialty(settings != null ? settings.getSpecialty() : null)
                .qualifications(settings != null ? settings.getQualifications() : null)
                .experienceYears(userProfileHelper.getExperienceYears(user))
                .consultationFee(settings != null ? settings.getConsultationFee() : null)
                .verificationStatus(
                        userProfileHelper.getVerificationStatus(user).name())
                .primaryClinicName(primary != null ? primary.getClinicName() : null)
                .primaryClinicCity(primary != null ? primary.getCity() : null)
                .availableForConsultation(
                        settings != null ? settings.getAvailableForConsultation() : null)
                .onlineConsultationEnabled(
                        settings != null ? settings.getOnlineConsultationEnabled() : null)
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            return List.of();
        }
    }
}
