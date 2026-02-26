package com.medsyncpro.repository;

import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, String> {
    List<VerificationRequest> findByStatusOrderByCreatedAtDesc(VerificationStatus status);
    List<VerificationRequest> findAllByOrderByCreatedAtDesc();
    Optional<VerificationRequest> findByUserId(String userId);
}
