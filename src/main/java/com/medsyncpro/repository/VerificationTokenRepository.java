package com.medsyncpro.repository;

import com.medsyncpro.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.id = ?1")
    void deleteByUserId(Long userId);
    
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.user.email = ?1 AND vt.createdAt > ?2")
    long countRecentTokensByEmail(String email, LocalDateTime since);
}
