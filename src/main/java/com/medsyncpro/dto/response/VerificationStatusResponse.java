package com.medsyncpro.dto.response;

import com.medsyncpro.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatusResponse {
    private VerificationStatus status;
    private List<DocumentResponse> submittedDocuments;
    private List<RequiredDocumentItem> requiredDocuments;
    private String verificationNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
