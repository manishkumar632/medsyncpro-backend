package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {
    private UUID id;

    // Doctor info
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorProfileImage;

    // Patient info
    private UUID patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    // Appointment details
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalTime endTime;
    private String type;
    private String status;
    private String symptoms;
    private String doctorNotes;
    private String diagnosis;
    private LocalDate followUpDate;
    private String cancellationReason;
    private String prescription;
}
