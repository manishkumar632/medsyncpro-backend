package com.medsyncpro.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DoseActionRequest {

    private String note;

    @Min(1)
    @Max(1440)
    private Integer snoozeMinutes;
}
