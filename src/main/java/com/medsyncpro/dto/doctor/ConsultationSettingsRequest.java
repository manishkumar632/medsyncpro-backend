package com.medsyncpro.dto.doctor;

import lombok.Data;

@Data
public class ConsultationSettingsRequest {
    private Integer slotDurationMinutes;
    private Integer followUpWindowDays;
    private String prescriptionTemplate;
    private Boolean autoApproveAppointments;
    private Boolean onlineConsultationEnabled;
}
