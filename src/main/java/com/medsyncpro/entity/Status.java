package com.medsyncpro.entity;

public enum Status {
    // Initial states
    PENDING,
    UPLOADED,

    // Review pipeline
    UNDER_REVIEW,
    ON_HOLD,
    REQUIRES_RESUBMISSION,

    // Final review outcomes
    APPROVED,
    REJECTED,

    // Delivery lifecycle
    DISPATCHED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    ACCEPTED,
    RETURNED,
    DELIVERY_FAILED,

    // Post-approval states
    EXPIRED,
    REVOKED,
    ARCHIVED
}
