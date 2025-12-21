package com.example.foreverhome.domain.pet;

/**
 * Represents the moderation status of pet content (name, description, images).
 * Pets must pass moderation before becoming publicly visible.
 */
public enum ModerationStatus {
    /**
     * Content has not yet been moderated by AI.
     */
    PENDING,

    /**
     * Content passed AI moderation checks.
     */
    APPROVED,

    /**
     * AI detected issues that require human review.
     */
    FLAGGED,

    /**
     * Content was rejected after human review.
     */
    REJECTED;

    /**
     * Returns true if this status blocks the pet from becoming publicly visible.
     * Only APPROVED status allows visibility.
     */
    public boolean isBlocking() {
        return this != APPROVED;
    }
}
