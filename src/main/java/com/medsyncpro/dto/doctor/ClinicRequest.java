package com.medsyncpro.dto.doctor;

import lombok.Data;

@Data
public class ClinicRequest {
    private String clinicName;
    private String address;
    private String city;
    private Boolean isPrimary;
}
