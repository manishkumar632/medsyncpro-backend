package com.medsyncpro.dto.doctor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClinicResponse {
    private String id;
    private String clinicName;
    private String address;
    private String city;
    private Boolean isPrimary;
}
