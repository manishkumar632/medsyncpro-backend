package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MedicationAdherenceSummaryResponse {

    private UUID patientId;
    private Double adherencePercentage;
    private long takenDoses;
    private long missedDoses;
    private long pendingDoses;
    private long totalEvaluatedDoses;
    private LocalDateTime from;
    private LocalDateTime to;
}
