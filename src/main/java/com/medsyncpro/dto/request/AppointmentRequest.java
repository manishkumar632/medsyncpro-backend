package com.medsyncpro.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class AppointmentRequest {
    private UUID doctorId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String type; // VIDEO, IN_PERSON, CHAT
    private String symptoms;
}
