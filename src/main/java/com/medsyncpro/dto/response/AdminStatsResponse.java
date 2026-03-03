package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {

    private RoleStats doctors;
    private RoleStats pharmacists;
    private RoleStats agents;
    private long totalPatients;

    @Data
    @Builder
    public static class RoleStats {
        private long verified;
        private long unverified;
    }
}
