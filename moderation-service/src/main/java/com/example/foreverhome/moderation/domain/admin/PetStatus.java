package com.example.foreverhome.moderation.domain.admin;

/**
 * Pet status enum for analytics queries.
 */
public enum PetStatus {
    DRAFT,
    PENDING_RESCUE,
    PENDING_VET,
    AVAILABLE,
    IN_PROGRESS,
    ON_HOLD,
    ADOPTED,
    WITHDRAWN
}
