package com.medsyncpro.dto.doctor;

import lombok.Data;
import java.util.Map;

@Data
public class AvailabilityRequest {
    private Boolean availableForConsultation;
    private Map<String, DaySchedule> weeklySchedule;

    @Data
    public static class DaySchedule {
        private boolean enabled;
        private java.util.List<TimeSlot> slots;
    }

    @Data
    public static class TimeSlot {
        private String start;
        private String end;
    }
}
