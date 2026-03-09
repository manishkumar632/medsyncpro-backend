package com.medsyncpro.dto.request;

import com.medsyncpro.entity.ReminderScheduleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateMedicationScheduleRequest {

    @NotBlank
    private String medicineName;

    private String dosage;

    private String frequency;

    private String instructions;

    @NotNull
    private ReminderScheduleType scheduleType = ReminderScheduleType.DAILY;

    @NotEmpty
    private List<String> reminderTimes;

    private List<String> reminderDays;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private UUID doctorId;

    private UUID prescriptionId;

    private Boolean reminderInApp = true;

    private Boolean reminderEmail = false;

    private Boolean reminderPush = false;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double adherenceAlertThreshold;
}
