package com.medsyncpro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medsyncpro.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByUserId(UUID userId);
    
    long countByIsVerifiedTrueAndDeletedFalse();

    long countByIsVerifiedFalseAndDeletedFalse();
}
