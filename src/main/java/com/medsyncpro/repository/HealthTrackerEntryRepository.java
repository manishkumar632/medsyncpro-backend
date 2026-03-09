package com.medsyncpro.repository;

import com.medsyncpro.entity.HealthMetricType;
import com.medsyncpro.entity.HealthTrackerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthTrackerEntryRepository extends JpaRepository<HealthTrackerEntry, UUID> {

    List<HealthTrackerEntry> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    List<HealthTrackerEntry> findByPatientIdAndMetricTypeOrderByRecordedAtDesc(
            UUID patientId, HealthMetricType metricType);

    Optional<HealthTrackerEntry> findByIdAndPatientId(UUID id, UUID patientId);
}
