package com.example.foreverhome.security;

import com.example.foreverhome.domain.user.UserRole;

import java.util.UUID;

/**
 * Principal class representing the authenticated user.
 * Supports impersonation where an admin can view the app as another user.
 */
public class UserPrincipal {
    private final UUID userId;
    private final UserRole role;
    private final boolean verified;
    private boolean impersonated;
    private UUID impersonatingAdminId;

    public UserPrincipal(UUID userId, UserRole role, boolean verified) {
        this.userId = userId;
        this.role = role;
        this.verified = verified;
        this.impersonated = false;
        this.impersonatingAdminId = null;
    }

    public UUID userId() {
        return userId;
    }

    public UserRole role() {
        return role;
    }

    public boolean verified() {
        return verified;
    }

    public boolean isImpersonated() {
        return impersonated;
    }

    public void setImpersonated(boolean impersonated) {
        this.impersonated = impersonated;
    }

    public UUID getImpersonatingAdminId() {
        return impersonatingAdminId;
    }

    public void setImpersonatingAdminId(UUID impersonatingAdminId) {
        this.impersonatingAdminId = impersonatingAdminId;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    public boolean isFoster() {
        return role == UserRole.FOSTER;
    }

    public boolean isAdopter() {
        return role == UserRole.ADOPTER;
    }

    public boolean isVet() {
        return role == UserRole.VET;
    }

    public boolean isRescueOrg() {
        return role == UserRole.RESCUE_ORG;
    }

    @Override
    public String toString() {
        if (impersonated) {
            return "UserPrincipal{userId=" + userId + ", role=" + role + ", impersonatedBy=" + impersonatingAdminId + "}";
        }
        return "UserPrincipal{userId=" + userId + ", role=" + role + "}";
    }
}
