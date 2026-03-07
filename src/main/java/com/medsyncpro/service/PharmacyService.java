package com.medsyncpro.service;

import com.medsyncpro.dto.response.PrescriptionResponse;
import com.medsyncpro.entity.Doctor;
import com.medsyncpro.entity.Patient;
import com.medsyncpro.entity.Prescription;
import com.medsyncpro.repository.PrescriptionRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyService {

    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getAllPrescriptions(Pageable pageable) {
        Page<Prescription> page = prescriptionRepository.findAll(pageable);
        List<PrescriptionResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getPrescriptionsByPatient(UUID patientId, Pageable pageable) {
        Page<Prescription> page = prescriptionRepository.findByPatientId(patientId, pageable);
        List<PrescriptionResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    private PrescriptionResponse toResponse(Prescription presc) {
        Doctor doctor = presc.getDoctor();
        Patient patient = presc.getPatient();
        com.medsyncpro.entity.Appointment appointment = presc.getAppointment();

        return PrescriptionResponse.builder()
                .id(presc.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                // .doctorSpecialty(doctor.getSpecialization() != null ?
                // doctor.getSpecialization().getName() : null)
                .doctorProfileImage(doctor.getProfileImage())
                .patientId(patient.getId())
                .patientName(patient.getName())
                .patientEmail(patient.getUser().getEmail())
                .appointmentId(appointment != null ? appointment.getId() : null)
                .appointmentDate(appointment != null ? appointment.getScheduledDate() : null)
                .medicines(presc.getMedicines())
                .notes(presc.getNotes())
                .build();
    }
}
