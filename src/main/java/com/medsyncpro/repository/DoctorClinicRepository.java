package com.medsyncpro.repository;

import com.medsyncpro.entity.DoctorClinic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorClinicRepository extends JpaRepository<DoctorClinic, String> {
    List<DoctorClinic> findByUserIdOrderByIsPrimaryDescCreatedAtAsc(String userId);
}
