package com.medsyncpro.repository;

import com.medsyncpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    User findByEmail(String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = ?1 AND u.deleted = false")
    boolean existsByEmailAndNotDeleted(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.email = ?1 AND u.createdAt > ?2")
    long countRecentRegistrationsByEmail(String email, LocalDateTime since);
}
