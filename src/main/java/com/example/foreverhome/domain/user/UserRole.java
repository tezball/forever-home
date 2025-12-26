package com.example.foreverhome.domain.user;

/**
 * Defines the roles available for users in the Forever Home platform.
 * Each role has specific capabilities and permissions.
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    FOSTER,
    ADOPTER,
    VET,
    RESCUE_ORG;

    /**
     * Checks if this role has administrative privileges.
     * @return true if the role is ADMIN or SUPER_ADMIN
     */
    public boolean isAdministrative() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    /**
     * Checks if this role is the super admin role.
     * Super admins can approve rescue organizations and view platform stats.
     * @return true if the role is SUPER_ADMIN
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * Checks if this role requires any form of verification before becoming active.
     * @return true if the role requires verification (VET or RESCUE_ORG)
     */
    public boolean requiresVerification() {
        return this == VET || this == RESCUE_ORG;
    }

    /**
     * Checks if this role requires admin verification before becoming active.
     * Only Rescue Organizations must be verified by admins.
     * @return true if the role requires admin verification
     */
    public boolean requiresAdminVerification() {
        return this == RESCUE_ORG;
    }

    /**
     * Checks if this role requires verification by a rescue organization.
     * Vets must be verified by rescue organizations before they can sign off on pets.
     * @return true if the role requires rescue verification
     */
    public boolean requiresRescueVerification() {
        return this == VET;
    }
}
