package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class PrescriptionResponse {
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

    // Appointment info
    private UUID appointmentId;
    private LocalDate appointmentDate;

    // Prescription details
    private String medicines;
    private String notes;
}
