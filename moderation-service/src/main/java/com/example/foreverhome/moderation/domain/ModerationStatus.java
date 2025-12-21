package com.example.foreverhome.moderation.domain;

/**
 * Status of a moderation result.
 */
public enum ModerationStatus {
    /** Moderation pending or errored, needs retry */
    PENDING,

    /** Content passed moderation checks */
    APPROVED,

    /** Content flagged for human review */
    FLAGGED,

    /** Content rejected after human review */
    REJECTED
}
