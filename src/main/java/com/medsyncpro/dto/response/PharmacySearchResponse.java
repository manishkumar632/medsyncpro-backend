package com.medsyncpro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PharmacySearchResponse {

    private UUID id;
    private String name;
    private String address;
    private String profileImage;
    private boolean verified;
}
