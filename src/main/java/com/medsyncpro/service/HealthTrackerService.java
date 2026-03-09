package com.medsyncpro.service;

import com.medsyncpro.dto.request.HealthTrackerEntryRequest;
import com.medsyncpro.dto.response.HealthTrackerEntryResponse;
import com.medsyncpro.entity.HealthMetricType;
import com.medsyncpro.entity.HealthTrackerEntry;
import com.medsyncpro.entity.Patient;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.HealthTrackerEntryRepository;
import com.medsyncpro.repository.PatientRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthTrackerService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final HealthTrackerEntryRepository healthTrackerEntryRepository;

    @Transactional
    public HealthTrackerEntryResponse addEntry(UUID patientUserId, HealthTrackerEntryRequest request) {
        Patient patient = getPatientByUserId(patientUserId);
        HealthTrackerEntry entry = HealthTrackerEntry.builder()
                .patient(patient)
                .metricType(request.getMetricType())
                .metricValue(request.getMetricValue().trim())
                .unit(request.getUnit())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        return toResponse(healthTrackerEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<HealthTrackerEntryResponse> getEntries(UUID patientUserId, HealthMetricType metricType) {
        Patient patient = getPatientByUserId(patientUserId);
        List<HealthTrackerEntry> entries = metricType == null
                ? healthTrackerEntryRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId())
                : healthTrackerEntryRepository.findByPatientIdAndMetricTypeOrderByRecordedAtDesc(
                        patient.getId(), metricType);
        return entries.stream().map(this::toResponse).toList();
    }

    private Patient getPatientByUserId(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    private HealthTrackerEntryResponse toResponse(HealthTrackerEntry entry) {
        return HealthTrackerEntryResponse.builder()
                .id(entry.getId())
                .metricType(entry.getMetricType())
                .metricValue(entry.getMetricValue())
                .unit(entry.getUnit())
                .recordedAt(entry.getRecordedAt())
                .notes(entry.getNotes())
                .build();
    }
}
