package com.medsyncpro.dto.response;

import com.medsyncpro.entity.HealthMetricType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HealthTrackerEntryResponse {

    private UUID id;
    private HealthMetricType metricType;
    private String metricValue;
    private String unit;
    private LocalDateTime recordedAt;
    private String notes;
}
