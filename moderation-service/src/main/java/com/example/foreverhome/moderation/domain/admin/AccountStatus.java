package com.example.foreverhome.moderation.domain.admin;

import java.util.EnumSet;
import java.util.Set;

public enum AccountStatus {
    PENDING,
    ACTIVE,
    SUSPENDED;

    private static final Set<AccountStatus> LOGIN_ALLOWED = EnumSet.of(ACTIVE);

    public boolean canLogin() {
        return LOGIN_ALLOWED.contains(this);
    }

    public boolean canTransitionTo(AccountStatus target) {
        return switch (this) {
            case PENDING -> target == ACTIVE;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> target == ACTIVE;
        };
    }
}
