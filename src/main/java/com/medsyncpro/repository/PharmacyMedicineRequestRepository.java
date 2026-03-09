package com.medsyncpro.repository;

import com.medsyncpro.entity.PharmacyMedicineRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmacyMedicineRequestRepository extends JpaRepository<PharmacyMedicineRequest, UUID> {

    Page<PharmacyMedicineRequest> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

    Page<PharmacyMedicineRequest> findByPharmacyIdOrderByCreatedAtDesc(UUID pharmacyId, Pageable pageable);

    Page<PharmacyMedicineRequest> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    Optional<PharmacyMedicineRequest> findByIdAndPatientId(UUID id, UUID patientId);

    Optional<PharmacyMedicineRequest> findByIdAndPharmacyId(UUID id, UUID pharmacyId);

    Optional<PharmacyMedicineRequest> findByIdAndAgentId(UUID id, UUID agentId);
}
