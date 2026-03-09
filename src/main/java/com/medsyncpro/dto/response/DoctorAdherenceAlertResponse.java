package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DoctorAdherenceAlertResponse {

    private UUID alertId;
    private UUID patientId;
    private String patientName;
    private Double adherencePercentage;
    private Double threshold;
    private LocalDateTime alertedAt;
}
