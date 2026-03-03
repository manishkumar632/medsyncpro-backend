package com.medsyncpro.repository;

import com.medsyncpro.entity.DoctorClinic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DoctorClinicRepository extends JpaRepository<DoctorClinic, UUID> {
    List<DoctorClinic> findByUserIdOrderByIsPrimaryDescCreatedAtAsc(UUID userId);
}
