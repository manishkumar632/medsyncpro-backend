package com.medsyncpro.dto.response;

import com.medsyncpro.entity.DoseStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MedicationDoseLogResponse {

    private UUID id;
    private UUID scheduleId;
    private String medicineName;
    private LocalDateTime scheduledAt;
    private LocalDateTime takenAt;
    private LocalDateTime snoozedUntil;
    private DoseStatus status;
    private String note;
}
