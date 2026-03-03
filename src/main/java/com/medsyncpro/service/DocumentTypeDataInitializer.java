package com.medsyncpro.service;

import com.medsyncpro.entity.DocumentTypeEntity;
import com.medsyncpro.entity.ModelDocumentType;
import com.medsyncpro.entity.UserModelType;
import com.medsyncpro.repository.DocumentTypeEntityRepository;
import com.medsyncpro.repository.ModelDocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds default document types and model mappings on first run.
 * Idempotent — skips creation if the code already exists.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeDataInitializer {

    private final DocumentTypeEntityRepository typeRepo;
    private final ModelDocumentTypeRepository mappingRepo;

    @Transactional
    public void seedDefaults() {
        log.info("Seeding default document types...");

        // ── Global document types ──
        DocumentTypeEntity license = ensureType("LICENSE", "Medical License");
        DocumentTypeEntity certificate = ensureType("CERTIFICATE", "Certificate");
        DocumentTypeEntity idProof = ensureType("ID_PROOF", "Government ID Proof");
        DocumentTypeEntity degree = ensureType("DEGREE", "Degree Certificate");
        DocumentTypeEntity boardCert = ensureType("BOARD_CERTIFICATION", "Board Certification");
        DocumentTypeEntity pharmacyLic = ensureType("PHARMACY_LICENSE", "Pharmacy License");
        DocumentTypeEntity stateCert = ensureType("STATE_CERTIFICATION", "State Certification");
        DocumentTypeEntity compliance = ensureType("COMPLIANCE_CERTIFICATE", "Compliance Certificate");
        DocumentTypeEntity deaReg = ensureType("DEA_REGISTRATION", "DEA Registration");
        DocumentTypeEntity profilePhoto = ensureType("PROFILE_PHOTO", "Profile Photo");
        DocumentTypeEntity other = ensureType("OTHER", "Other");

        // ── Doctor defaults ──
        ensureMapping(UserModelType.DOCTOR, license, true, 1);
        ensureMapping(UserModelType.DOCTOR, idProof, true, 2);
        ensureMapping(UserModelType.DOCTOR, degree, true, 3);
        ensureMapping(UserModelType.DOCTOR, boardCert, false, 4);
        ensureMapping(UserModelType.DOCTOR, deaReg, false, 5);

        // ── Pharmacist defaults ──
        ensureMapping(UserModelType.PHARMACIST, pharmacyLic, true, 1);
        ensureMapping(UserModelType.PHARMACIST, stateCert, true, 2);
        ensureMapping(UserModelType.PHARMACIST, idProof, true, 3);
        ensureMapping(UserModelType.PHARMACIST, compliance, false, 4);

        // ── Agent defaults ──
        ensureMapping(UserModelType.AGENT, idProof, true, 1);

        log.info("Document type seeding complete.");
    }

    private DocumentTypeEntity ensureType(String code, String name) {
        return typeRepo.findByCodeAndDeletedFalse(code)
                .orElseGet(() -> {
                    DocumentTypeEntity dt = DocumentTypeEntity.builder()
                            .code(code)
                            .name(name)
                            .active(true)
                            .build();
                    log.info("  Created document type: {} ({})", name, code);
                    return typeRepo.save(dt);
                });
    }

    private void ensureMapping(UserModelType model, DocumentTypeEntity type,
            boolean required, int order) {
        if (!mappingRepo.existsByModelTypeAndDocumentTypeAndDeletedFalse(model, type)) {
            ModelDocumentType mapping = ModelDocumentType.builder()
                    .modelType(model)
                    .documentType(type)
                    .required(required)
                    .active(true)
                    .displayOrder(order)
                    .build();
            mappingRepo.save(mapping);
            log.info("  Mapped {} → {} (required={})", model, type.getName(), required);
        }
    }
}
