package com.example.foreverhome.domain.adoption;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the status of an adoption application.
 */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    WITHDRAWN,
    FINALIZED;

    private static final Set<ApplicationStatus> ACTIVE = EnumSet.of(SUBMITTED, UNDER_REVIEW);
    private static final Set<ApplicationStatus> WITHDRAWABLE = EnumSet.of(SUBMITTED, UNDER_REVIEW);

    /**
     * Checks if this application is still active (not yet decided or withdrawn).
     * @return true if the application is active
     */
    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    /**
     * Checks if the applicant can withdraw the application.
     * Can only withdraw if not yet approved.
     * @return true if withdrawal is allowed
     */
    public boolean canWithdraw() {
        return WITHDRAWABLE.contains(this);
    }

    /**
     * Checks if this status can transition to the target status.
     * @param target the target status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        return switch (this) {
            case SUBMITTED -> target == UNDER_REVIEW || target == APPROVED || target == REJECTED || target == WITHDRAWN;
            case UNDER_REVIEW -> target == APPROVED || target == REJECTED || target == WITHDRAWN;
            case APPROVED -> target == FINALIZED;
            case REJECTED, WITHDRAWN, FINALIZED -> false;
        };
    }
}
