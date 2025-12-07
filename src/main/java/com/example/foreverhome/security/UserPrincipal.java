package com.example.foreverhome.security;

import com.example.foreverhome.domain.user.UserRole;

import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        UserRole role,
        boolean verified
) {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
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
}
