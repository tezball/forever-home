package com.example.foreverhome.domain.verification;

/**
 * Status of a vet's request for approval from a rescue organization.
 */
public enum VetApprovalRequestStatus {
    /**
     * Request has been submitted and is awaiting review.
     */
    PENDING,

    /**
     * Request has been approved by the rescue organization.
     */
    APPROVED,

    /**
     * Request has been rejected by the rescue organization.
     * Vet can submit a new request after rejection.
     */
    REJECTED,

    /**
     * Request was withdrawn by the vet before a decision was made.
     */
    WITHDRAWN
}
