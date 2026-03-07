package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlotResponse {
    private String date;
    private String time;
    private boolean available;
}
