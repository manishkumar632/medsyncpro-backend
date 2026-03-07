package com.medsyncpro.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

       // ─────────────────────────────────────────────
       // Basic Auth Queries
       // ─────────────────────────────────────────────

       boolean existsByEmail(String email);

       boolean existsByRoleAndDeletedFalse(Role role);

       Optional<User> findByEmail(String email);

       long countByRoleAndDeletedFalse(Role role);

       Page<User> findByRole(Role role, Pageable pageable);

       Page<User> findByRoleAndEmailContainingIgnoreCase(Role role, String email, Pageable pageable);

       @Query("""
                     SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
                     FROM User u
                     WHERE u.email = :email
                     AND u.deleted = false
                            """)
       boolean existsByEmailAndNotDeleted(@Param("email") String email);

       @Query("""
                     SELECT COUNT(u)
                     FROM User u
                     WHERE u.email = :email
                     AND u.createdAt > :since
                     AND u.deleted = false
                            """)
       long countRecentRegistrationsByEmail(
                     @Param("email") String email,
                     @Param("since") LocalDateTime since);

       // ─────────────────────────────────────────────
       // Admin User Listing
       // (User entity only contains auth fields)
       // ─────────────────────────────────────────────

       @Query("""
                     SELECT u
                     FROM User u
                     WHERE u.deleted = false
                     AND u.role = :role
                            """)
       Page<User> findByRoleAndDeletedFalse(
                     @Param("role") Role role,
                     Pageable pageable);

       @Query("""
                     SELECT u
                     FROM User u
                     WHERE u.deleted = false
                     AND u.role = :role
                     AND (
                            LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR u.phone LIKE CONCAT('%', :search, '%')
                     )
                            """)
       Page<User> findByRoleAndSearch(
                     @Param("role") Role role,
                     @Param("search") String search,
                     Pageable pageable);

       // ─────────────────────────────────────────────
       // Pending Approvals (Auth-level only)
       // Since verification status is NOT in User,
       // we can only filter by emailVerified + role
       // ─────────────────────────────────────────────

       @Query("""
                     SELECT u
                     FROM User u
                     WHERE u.emailVerified = true
                     AND u.deleted = false
                     AND u.role <> com.medsyncpro.entity.Role.ADMIN
                            """)
       List<User> findEmailVerifiedNonAdmins();

}