package com.example.foreverhome.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity representing a user in the Forever Home platform.
 * All specific user types (Foster, Adopter, Vet, RescueOrg, Admin)
 * have a one-to-one relationship with this entity.
 */
@Table("app_users")
public class User implements Persistable<UUID> {

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

    @Column("email_verification_token")
    private String emailVerificationToken;

    @Column("password_reset_token")
    private String passwordResetToken;

    @Column("password_reset_token_expiry")
    private Instant passwordResetTokenExpiry;

    @Column("failed_login_attempts")
    private int failedLoginAttempts;

    @Column("locked_until")
    private Instant lockedUntil;

    @Embedded.Nullable(prefix = "notif_")
    private NotificationPreferences notificationPreferences;

    @Transient
    private boolean isNew = false;

    // Private constructor for Spring Data JDBC
    protected User() {
    }

    private User(UUID id, String email, String name, String passwordHash, UserRole role,
                 AccountStatus status, boolean profileComplete, Instant createdAt,
                 Instant lastLoginAt, NotificationPreferences notificationPreferences) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.profileComplete = profileComplete;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.notificationPreferences = notificationPreferences;
        this.failedLoginAttempts = 0;
        this.isNew = true;
    }

    /**
     * Factory method to create a new user.
     * New users start with PENDING status and default notification preferences.
     *
     * @param email the user's email address
     * @param passwordHash the hashed password
     * @param role the user's role
     * @return a new User instance
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public static User create(String email, String passwordHash, UserRole role) {
        return create(email, passwordHash, role, null);
    }

    /**
     * Factory method to create a new user with a name.
     * New users start with PENDING status and default notification preferences.
     *
     * @param email the user's email address
     * @param passwordHash the hashed password
     * @param role the user's role
     * @param name the user's display name
     * @return a new User instance
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public static User create(String email, String passwordHash, UserRole role, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("password hash cannot be null or blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }

        return new User(
                UUID.randomUUID(),
                email.trim().toLowerCase(),
                name,
                passwordHash,
                role,
                AccountStatus.PENDING,
                false,
                Instant.now(),
                null,
                NotificationPreferences.defaults()
        );
    }

    // Getters
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Marks the entity as no longer new (after initial save).
     * This is needed when the entity is saved multiple times in a transaction.
     */
    public void markNotNew() {
        this.isNew = false;
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

    public NotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public Instant getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    // Business methods

    /**
     * Activates a pending user account (after email verification).
     * @throws IllegalStateException if the user cannot be activated from current status
     */
    public void activate() {
        if (!status.canTransitionTo(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot activate user from status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Suspends an active user account (admin action).
     * @throws IllegalStateException if the user cannot be suspended from current status
     */
    public void suspend() {
        if (!status.canTransitionTo(AccountStatus.SUSPENDED)) {
            throw new IllegalStateException("Cannot suspend user from status: " + status);
        }
        this.status = AccountStatus.SUSPENDED;
    }

    /**
     * Reactivates a suspended user account (admin action).
     * @throws IllegalStateException if the user cannot be reactivated from current status
     */
    public void reactivate() {
        if (!status.canTransitionTo(AccountStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot reactivate user from status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Marks the user's profile as complete.
     */
    public void markProfileComplete() {
        this.profileComplete = true;
    }

    /**
     * Records a successful login, updating the lastLoginAt timestamp.
     * @throws IllegalStateException if the user cannot login
     */
    public void recordLogin() {
        if (!canLogin()) {
            throw new IllegalStateException("Cannot login: user status is " + status);
        }
        this.lastLoginAt = Instant.now();
    }

    /**
     * Checks if the user can login based on their account status.
     * @return true if the user can login
     */
    public boolean canLogin() {
        return status.canLogin();
    }

    /**
     * Updates the user's notification preferences.
     * @param preferences the new notification preferences
     * @throws IllegalArgumentException if preferences is null
     */
    public void updateNotificationPreferences(NotificationPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("Notification preferences cannot be null");
        }
        this.notificationPreferences = preferences;
    }

    /**
     * Checks if this user's role requires admin verification.
     * @return true if verification is required (VET or RESCUE_ORG)
     */
    public boolean requiresVerification() {
        return role.requiresVerification();
    }

    /**
     * Updates the user's password hash.
     * @param newPasswordHash the new hashed password
     * @throws IllegalArgumentException if the hash is null or blank
     */
    public void updatePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }
        this.passwordHash = newPasswordHash;
    }

    /**
     * Sets the email verification token.
     * @param token the verification token
     */
    public void setEmailVerificationToken(String token) {
        this.emailVerificationToken = token;
    }

    /**
     * Clears the email verification token (after successful verification).
     */
    public void clearEmailVerificationToken() {
        this.emailVerificationToken = null;
    }

    /**
     * Sets the password reset token and its expiry.
     * @param token the reset token
     */
    public void setPasswordResetToken(String token) {
        this.passwordResetToken = token;
    }

    /**
     * Sets the password reset token expiry time.
     * @param expiry the expiry instant
     */
    public void setPasswordResetTokenExpiry(Instant expiry) {
        this.passwordResetTokenExpiry = expiry;
    }

    /**
     * Clears password reset token and expiry (after successful reset).
     */
    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpiry = null;
    }

    /**
     * Records a failed login attempt.
     * Increments the counter and locks account after 5 attempts.
     */
    public void recordFailedLoginAttempt() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES);
        }
    }

    /**
     * Resets failed login attempts (on successful login or after lockout expires).
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Checks if the account is currently locked.
     * @return true if locked
     */
    public boolean isLocked() {
        if (lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(lockedUntil)) {
            // Lock expired, reset
            resetFailedLoginAttempts();
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", profileComplete=" + profileComplete +
                '}';
    }
}
