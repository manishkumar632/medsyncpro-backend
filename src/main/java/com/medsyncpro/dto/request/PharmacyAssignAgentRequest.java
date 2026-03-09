package com.medsyncpro.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PharmacyAssignAgentRequest {

    @NotNull
    private UUID agentId;

    private String note;
}
