package com.medsyncpro.repository;

import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationRequest;
import com.medsyncpro.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository
        extends JpaRepository<VerificationRequest, String> {

    // Single status
    List<VerificationRequest> findByStatusOrderByCreatedAtDesc(
            VerificationStatus status);

    // Multiple statuses (IN query) ✅ ADD THIS
    List<VerificationRequest> findByStatusInOrderByCreatedAtDesc(
            List<VerificationStatus> statuses);

    List<VerificationRequest> findAllByOrderByCreatedAtDesc();

    Optional<VerificationRequest> findTopByUserOrderByCreatedAtDesc(User user);

    Optional<VerificationRequest> findByUserId(UUID userId);
}