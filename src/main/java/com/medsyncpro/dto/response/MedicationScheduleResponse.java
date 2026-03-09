package com.medsyncpro.dto.response;

import com.medsyncpro.entity.ReminderScheduleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MedicationScheduleResponse {

    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID prescriptionId;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String instructions;
    private ReminderScheduleType scheduleType;
    private List<String> reminderTimes;
    private List<String> reminderDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean reminderInApp;
    private Boolean reminderEmail;
    private Boolean reminderPush;
    private Double adherenceAlertThreshold;
    private Boolean active;
}
