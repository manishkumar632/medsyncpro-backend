package com.medsyncpro.repository;

import com.medsyncpro.entity.MedicationAdherenceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationAdherenceAlertRepository extends JpaRepository<MedicationAdherenceAlert, UUID> {

    Optional<MedicationAdherenceAlert> findTopByPatientIdAndDoctorIdOrderByAlertedAtDesc(
            UUID patientId, UUID doctorId);

    List<MedicationAdherenceAlert> findByDoctorIdOrderByAlertedAtDesc(UUID doctorId);
}
