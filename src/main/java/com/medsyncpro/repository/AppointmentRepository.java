package com.medsyncpro.repository;

import com.medsyncpro.entity.Appointment;
import com.medsyncpro.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByDoctorUserIdOrderByScheduledDateDescScheduledTimeDesc(
            UUID doctorUserId, Pageable pageable);

    Page<Appointment> findByPatientIdOrderByScheduledDateDescScheduledTimeDesc(
            UUID patientId, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.scheduledDate = :date " +
            "AND a.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND ((a.scheduledTime < :endTime AND a.endTime > :startTime))")
    List<Appointment> findConflicting(
            @Param("doctorId") UUID doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.scheduledDate = :date " +
            "AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    List<Appointment> findByDoctorIdAndScheduledDate(
            @Param("doctorId") UUID doctorId,
            @Param("date") LocalDate date);

    long countByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status);
}
