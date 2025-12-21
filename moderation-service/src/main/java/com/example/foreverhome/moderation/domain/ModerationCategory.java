package com.example.foreverhome.moderation.domain;

/**
 * Categories of content moderation flags.
 */
public enum ModerationCategory {
    /** Profanity, offensive language, or adult content */
    INAPPROPRIATE,

    /** False claims about pet health, breed, or characteristics */
    MISLEADING,

    /** Advertising, promotional content, or irrelevant information */
    SPAM,

    /** Requests for money, suspicious contact information, or fraud indicators */
    SCAM,

    /** Content promoting animal abuse or neglect */
    HARMFUL,

    /** Image does not contain a pet */
    NOT_PET,

    /** Image is not family-safe or contains inappropriate content */
    NSFW,

    /** Image is unclear or low quality */
    UNCLEAR,

    /** Other issues not covered by specific categories */
    OTHER
}
