package com.medsyncpro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsResponse {
    private long totalPatients;
    private long totalDoctors;
    private long totalPharmacists;
    private long totalUsers;
    private long pendingApprovals;
}
