package com.medsyncpro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PharmacyMedicineRequestCreateRequest {

    @NotNull
    private UUID pharmacyId;

    private UUID prescriptionId;

    @NotBlank
    private String deliveryAddress;

    private String note;
}
