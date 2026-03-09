package com.medsyncpro.dto.request;

import com.medsyncpro.entity.HealthMetricType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthTrackerEntryRequest {

    @NotNull
    private HealthMetricType metricType;

    @NotBlank
    private String metricValue;

    private String unit;

    private LocalDateTime recordedAt;

    private String notes;
}
