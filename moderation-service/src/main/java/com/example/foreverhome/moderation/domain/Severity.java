package com.example.foreverhome.moderation.domain;

/**
 * Severity level of a moderation flag.
 */
public enum Severity {
    /** Minor issue, may be acceptable */
    LOW,

    /** Moderate issue, should be reviewed */
    MEDIUM,

    /** Serious issue, likely requires action */
    HIGH
}
