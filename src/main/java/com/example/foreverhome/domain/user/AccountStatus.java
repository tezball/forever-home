package com.example.foreverhome.domain.user;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the account status of a user in the system.
 */
public enum AccountStatus {
    PENDING,
    ACTIVE,
    SUSPENDED;

    private static final Set<AccountStatus> LOGIN_ALLOWED = EnumSet.of(ACTIVE);

    /**
     * Checks if a user with this status can log in.
     * Only ACTIVE users can log in.
     * @return true if login is allowed
     */
    public boolean canLogin() {
        return LOGIN_ALLOWED.contains(this);
    }

    /**
     * Checks if this status can transition to the target status.
     * Valid transitions:
     * - PENDING -> ACTIVE (email verified)
     * - ACTIVE -> SUSPENDED (admin action)
     * - SUSPENDED -> ACTIVE (admin reactivation)
     * @param target the target status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(AccountStatus target) {
        return switch (this) {
            case PENDING -> target == ACTIVE;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> target == ACTIVE;
        };
    }
}
