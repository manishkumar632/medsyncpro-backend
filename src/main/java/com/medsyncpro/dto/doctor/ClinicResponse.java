package com.medsyncpro.dto.doctor;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight DTO representing a single clinic entry
 * returned inside DoctorPublicProfile and search results.
 */
@Data
@Builder
public class ClinicResponse {

    private String id;
    private String clinicName;
    private String address;
    private String city;
    private Boolean isPrimary;
}