package com.medsyncpro.repository;

import com.medsyncpro.entity.Patient;
import com.medsyncpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUserId(UUID userId);
}
