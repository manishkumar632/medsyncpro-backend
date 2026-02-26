package com.medsyncpro.dto;

import com.medsyncpro.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatusResponse {
    private VerificationStatus status;
    private List<DocumentResponse> submittedDocuments;
    private String verificationNotes;
}
