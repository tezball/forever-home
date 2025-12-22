package com.example.foreverhome.moderation.domain.admin;

public enum UserRole {
    ADMIN,
    FOSTER,
    ADOPTER,
    VET,
    RESCUE_ORG;

    public boolean isAdministrative() {
        return this == ADMIN;
    }

    public boolean requiresVerification() {
        return this == VET || this == RESCUE_ORG;
    }

    public boolean requiresAdminVerification() {
        return this == RESCUE_ORG;
    }
}
