package com.medsyncpro.repository;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    User findByEmail(String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = ?1 AND u.deleted = false")
    boolean existsByEmailAndNotDeleted(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.email = ?1 AND u.createdAt > ?2")
    long countRecentRegistrationsByEmail(String email, LocalDateTime since);
    
    long countByRoleAndDeletedFalse(Role role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true AND u.approved = false AND u.deleted = false AND u.role <> com.medsyncpro.entity.Role.ADMIN")
    long countPendingApprovals();
}
