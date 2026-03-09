package com.medsyncpro.repository;

import com.medsyncpro.entity.DoseStatus;
import com.medsyncpro.entity.MedicationDoseLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationDoseLogRepository extends JpaRepository<MedicationDoseLog, UUID> {

    boolean existsByScheduleIdAndScheduledAt(UUID scheduleId, LocalDateTime scheduledAt);

    Optional<MedicationDoseLog> findByIdAndPatientId(UUID id, UUID patientId);

    List<MedicationDoseLog> findByPatientIdAndScheduledAtBetweenOrderByScheduledAtDesc(
            UUID patientId, LocalDateTime from, LocalDateTime to);

    Page<MedicationDoseLog> findByPatientIdOrderByScheduledAtDesc(UUID patientId, Pageable pageable);

    List<MedicationDoseLog> findByStatusAndScheduledAtBefore(DoseStatus status, LocalDateTime before);

    List<MedicationDoseLog> findByStatusAndSnoozedUntilLessThanEqual(DoseStatus status, LocalDateTime time);

    long countByPatientIdAndScheduledAtBetweenAndStatus(
            UUID patientId, LocalDateTime from, LocalDateTime to, DoseStatus status);

    long countByPatientIdAndScheduledAtBetweenAndStatusIn(
            UUID patientId, LocalDateTime from, LocalDateTime to, List<DoseStatus> statuses);
}
