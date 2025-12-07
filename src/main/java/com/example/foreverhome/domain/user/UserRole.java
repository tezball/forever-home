package com.example.foreverhome.domain.user;

/**
 * Defines the roles available for users in the Forever Home platform.
 * Each role has specific capabilities and permissions.
 */
public enum UserRole {
    ADMIN,
    FOSTER,
    ADOPTER,
    VET,
    RESCUE_ORG;

    /**
     * Checks if this role has administrative privileges.
     * @return true if the role is ADMIN
     */
    public boolean isAdministrative() {
        return this == ADMIN;
    }

    /**
     * Checks if this role requires admin verification before becoming active.
     * Vets and Rescue Organizations must be verified by admins.
     * @return true if the role requires verification
     */
    public boolean requiresVerification() {
        return this == VET || this == RESCUE_ORG;
    }
}
