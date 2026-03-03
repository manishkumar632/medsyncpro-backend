package com.medsyncpro.repository;

import com.medsyncpro.entity.Admin;
import com.medsyncpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByUser(User user);
    Optional<Admin> findByUserId(UUID userId);
}
