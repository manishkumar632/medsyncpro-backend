package com.medsyncpro.repository;

import com.medsyncpro.entity.DoctorSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DoctorSettingsRepository extends JpaRepository<DoctorSettings, UUID> {
    Optional<DoctorSettings> findByUserId(UUID userId);
}
