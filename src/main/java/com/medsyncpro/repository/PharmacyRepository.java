package com.medsyncpro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.medsyncpro.entity.Pharmacy;

public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {
    Optional<Pharmacy> findByIdAndDeletedFalse(UUID id);

    long countByIsVerifiedTrueAndDeletedFalse();

    long countByIsVerifiedFalseAndDeletedFalse();
    
    java.util.Optional<Pharmacy> findByUser(com.medsyncpro.entity.User user);

    Optional<Pharmacy> findByUserId(UUID userId);

    @Query("""
            SELECT p FROM Pharmacy p
            WHERE p.isVerified = true
            AND (
                :q IS NULL OR :q = '' OR
                LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (
                :location IS NULL OR :location = '' OR
                LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', :location, '%'))
            )
            ORDER BY p.name ASC
            """)
    Page<Pharmacy> searchVerifiedPharmacies(
            @Param("q") String q,
            @Param("location") String location,
            Pageable pageable);
}
