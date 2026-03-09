package com.medsyncpro.repository;

import com.medsyncpro.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, UUID> {

    List<MedicationSchedule> findByPatientIdAndActiveTrueOrderByCreatedAtDesc(UUID patientId);

    List<MedicationSchedule> findByDoctorIdAndActiveTrueOrderByCreatedAtDesc(UUID doctorId);

    Optional<MedicationSchedule> findByIdAndPatientId(UUID id, UUID patientId);

    @Query("SELECT ms FROM MedicationSchedule ms WHERE ms.active = true " +
            "AND ms.startDate <= :today " +
            "AND (ms.endDate IS NULL OR ms.endDate >= :today)")
    List<MedicationSchedule> findActiveForDate(@Param("today") LocalDate today);

    List<MedicationSchedule> findByActiveTrue();
}
