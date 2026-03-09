package com.medsyncpro.dto.request;

import com.medsyncpro.entity.ReminderScheduleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateMedicationScheduleRequest {

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

    private Boolean active;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double adherenceAlertThreshold;
}
