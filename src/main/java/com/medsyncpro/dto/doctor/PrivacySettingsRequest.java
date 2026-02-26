package com.medsyncpro.dto.doctor;

import lombok.Data;
import java.util.Map;

@Data
public class PrivacySettingsRequest {
    private Map<String, Boolean> settings;
}
