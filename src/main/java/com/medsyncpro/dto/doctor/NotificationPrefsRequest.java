package com.medsyncpro.dto.doctor;

import lombok.Data;
import java.util.Map;

@Data
public class NotificationPrefsRequest {
    private Map<String, Boolean> prefs;
}
