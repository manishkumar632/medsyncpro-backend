package com.medsyncpro.repository;

import com.medsyncpro.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    Page<Prescription> findByPatientId(UUID patientId, Pageable pageable);

    Page<Prescription> findByDoctorId(UUID doctorId, Pageable pageable);

    Optional<Prescription> findByAppointmentId(UUID appointmentId);
}
