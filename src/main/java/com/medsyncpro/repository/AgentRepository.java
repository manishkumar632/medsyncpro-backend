package com.medsyncpro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medsyncpro.entity.Agent;

public interface AgentRepository extends JpaRepository<Agent, UUID> {
    Optional<Agent> findByIdAndDeletedFalse(UUID id);

    long countByIsVerifiedTrueAndDeletedFalse();

    long countByIsVerifiedFalseAndDeletedFalse();
    java.util.Optional<Agent> findByUser(com.medsyncpro.entity.User user);

    Optional<Agent> findByUserId(UUID userId);
}
