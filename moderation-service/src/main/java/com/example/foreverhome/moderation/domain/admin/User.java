package com.example.foreverhome.moderation.domain.admin;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity for admin operations.
 * Maps to the same app_users table as the main application.
 */
@Table("app_users")
public class User {

    @Id
    private UUID id;

    @Column("email")
    private String email;

    @Column("name")
    private String name;

    @Column("password_hash")
    private String passwordHash;

    @Column("role")
    private UserRole role;

    @Column("status")
    private AccountStatus status;

    @Column("profile_complete")
    private boolean profileComplete;

    @Column("created_at")
    private Instant createdAt;

    @Column("last_login_at")
    private Instant lastLoginAt;

    protected User() {
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void activate() {
        if (!status.canTransitionTo(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot activate user from status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void suspend() {
        if (!status.canTransitionTo(AccountStatus.SUSPENDED)) {
            throw new IllegalStateException("Cannot suspend user from status: " + status);
        }
        this.status = AccountStatus.SUSPENDED;
    }

    public void reactivate() {
        if (!status.canTransitionTo(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot reactivate user from status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void updatePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        this.passwordHash = newPasswordHash;
    }

    public String getDisplayName() {
        return name != null ? name : email;
    }
}
