package com.medsyncpro.dto.response;

import com.medsyncpro.entity.PharmacyRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PharmacyMedicineRequestResponse {

    private UUID id;
    private PharmacyRequestStatus status;
    private UUID patientId;
    private String patientName;
    private UUID pharmacyId;
    private String pharmacyName;
    private UUID prescriptionId;
    private UUID agentId;
    private String agentName;
    private String patientNote;
    private String pharmacyNote;
    private String deliveryAddress;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
