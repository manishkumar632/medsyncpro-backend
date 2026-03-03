package com.medsyncpro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medsyncpro.entity.Pharmacy;

public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {
    Optional<Pharmacy> findByIdAndDeletedFalse(UUID id);

    long countByIsVerifiedTrueAndDeletedFalse();

    long countByIsVerifiedFalseAndDeletedFalse();
    
    java.util.Optional<Pharmacy> findByUser(com.medsyncpro.entity.User user);
}
