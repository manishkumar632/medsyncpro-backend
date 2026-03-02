package com.medsyncpro.repository;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import com.medsyncpro.entity.VerificationStatus;

public interface UserRepository extends JpaRepository<User, String> {
       boolean existsByEmail(String email);

       User findByEmail(String email);

       @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = ?1 AND u.deleted = false")
       boolean existsByEmailAndNotDeleted(String email);

       @Query("SELECT COUNT(u) FROM User u WHERE u.email = ?1 AND u.createdAt > ?2")
       long countRecentRegistrationsByEmail(String email, LocalDateTime since);

       long countByRoleAndDeletedFalse(Role role);

       @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true AND u.professionalVerificationStatus IN (com.medsyncpro.entity.VerificationStatus.UNDER_REVIEW, com.medsyncpro.entity.VerificationStatus.DOCUMENT_SUBMITTED) AND u.deleted = false AND u.role <> com.medsyncpro.entity.Role.ADMIN")
       long countPendingApprovals();

       // ── Admin user listing queries ──

       @Query("SELECT u FROM User u WHERE u.deleted = false AND u.role = :role")
       Page<User> findByRoleAndDeletedFalse(@Param("role") Role role, Pageable pageable);

       @Query("SELECT u FROM User u WHERE u.deleted = false AND u.role = :role " +
                     "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
       Page<User> findByRoleAndSearch(@Param("role") Role role, @Param("search") String search, Pageable pageable);

       @Query("SELECT u FROM User u WHERE u.deleted = false AND u.role = :role AND u.professionalVerificationStatus = :status")
       Page<User> findByRoleAndVerificationStatus(@Param("role") Role role, @Param("status") VerificationStatus status,
                     Pageable pageable);

       @Query("SELECT u FROM User u WHERE u.deleted = false AND u.role = :role AND u.professionalVerificationStatus = :status "
                     +
                     "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
       Page<User> findByRoleAndVerificationStatusAndSearch(@Param("role") Role role,
                     @Param("status") VerificationStatus status, @Param("search") String search, Pageable pageable);

       // Pending approvals list (email verified but not approved, non-admin)
       @Query("SELECT u FROM User u WHERE u.emailVerified = true AND u.professionalVerificationStatus IN (com.medsyncpro.entity.VerificationStatus.UNDER_REVIEW, com.medsyncpro.entity.VerificationStatus.DOCUMENT_SUBMITTED) AND u.deleted = false AND u.role <> com.medsyncpro.entity.Role.ADMIN")
       List<User> findPendingApprovals();

       // ── Doctor public search queries (used by DoctorSearchService) ──

       /**
        * Search verified doctors by name, email, or phone number.
        * Specialty and clinic fields are filtered in-memory in DoctorSearchService
        * after the initial DB fetch, since those fields live in separate tables.
        */
       @Query("SELECT DISTINCT u FROM User u " +
                     "LEFT JOIN DoctorSettings ds ON ds.userId = u.id " +
                     "LEFT JOIN DoctorClinic dc ON dc.userId = u.id " +
                     "WHERE u.deleted = false " +
                     "AND u.role = :role " +
                     "AND u.professionalVerificationStatus = :status " +
                     "AND (" +
                     "  LOWER(u.name)  LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  u.phone        LIKE CONCAT('%', :search, '%')        OR " +
                     "  LOWER(u.city)  LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(u.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(ds.specialty) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(ds.qualifications) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(dc.clinicName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "  LOWER(dc.city) LIKE LOWER(CONCAT('%', :search, '%')) " +
                     ")")
       Page<User> findVerifiedDoctorsBySearch(
                     @Param("role") Role role,
                     @Param("status") VerificationStatus status,
                     @Param("search") String search,
                     Pageable pageable);
}