package com.example.foreverhome.moderation.domain;

/**
 * Status of a batch moderation job.
 */
public enum JobStatus {
    /** Job created but not started */
    PENDING,

    /** Job is currently running */
    RUNNING,

    /** Job completed successfully */
    COMPLETED,

    /** Job failed with error */
    FAILED,

    /** Job was cancelled */
    CANCELLED
}
