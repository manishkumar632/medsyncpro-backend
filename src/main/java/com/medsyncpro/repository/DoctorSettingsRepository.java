package com.medsyncpro.repository;

import com.medsyncpro.entity.DoctorSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DoctorSettingsRepository extends JpaRepository<DoctorSettings, String> {
    Optional<DoctorSettings> findByUserId(String userId);
}
