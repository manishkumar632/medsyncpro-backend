package com.medsyncpro.dto.request;

import com.medsyncpro.entity.PharmacyRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PharmacyRequestStatusUpdateRequest {

    @NotNull
    private PharmacyRequestStatus status;

    private String note;
}
