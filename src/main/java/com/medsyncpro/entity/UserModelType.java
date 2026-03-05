package com.medsyncpro.entity;

/**
 * Identifies user models that participate in document-based verification.
 * Adding a new model requires only a new enum constant here — no schema
 * changes.
 */
public enum UserModelType {
    DOCTOR,
    PHARMACIST,
    AGENT,
    PATIENT
}
