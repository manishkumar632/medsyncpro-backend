package com.medsyncpro.entity;

/**
 * @deprecated Use {@link DocumentTypeEntity} (database-driven) instead.
 *             Kept temporarily for reference during migration.
 */
@Deprecated
public enum DocumentType {
    LICENSE,
    CERTIFICATE,
    ID_PROOF,
    DEGREE,
    BOARD_CERTIFICATION,
    PROFILE_PHOTO,
    PRESCRIPTION,
    MEDICAL_REPORT,
    OTHER
}
