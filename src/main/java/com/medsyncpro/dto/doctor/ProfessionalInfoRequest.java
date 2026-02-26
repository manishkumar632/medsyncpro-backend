package com.medsyncpro.dto.doctor;

import lombok.Data;
import java.util.List;

@Data
public class ProfessionalInfoRequest {
    private String specialty;
    private String qualifications;
    private Integer experienceYears;
    private String medRegNumber;
    private Double consultationFee;
    private List<String> languages;
    private List<String> expertise;
}
