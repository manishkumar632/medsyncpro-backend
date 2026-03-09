package com.medsyncpro.repository;

import com.medsyncpro.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByDoctorIdAndPatientId(UUID doctorId, UUID patientId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.doctor.id = :doctorId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findByDoctorIdOrderByActivity(@Param("doctorId") UUID doctorId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.patient.id = :patientId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findByPatientIdOrderByActivity(@Param("patientId") UUID patientId);

    @Query("SELECT COALESCE(SUM(c.doctorUnreadCount), 0) FROM Conversation c WHERE c.doctor.id = :doctorId")
    long sumDoctorUnread(@Param("doctorId") UUID doctorId);

    @Query("SELECT COALESCE(SUM(c.patientUnreadCount), 0) FROM Conversation c WHERE c.patient.id = :patientId")
    long sumPatientUnread(@Param("patientId") UUID patientId);
}