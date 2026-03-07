package com.medsyncpro.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ResubmitRequestDTO {
    private String comment;
    private List<String> documentTypeCodes;
}