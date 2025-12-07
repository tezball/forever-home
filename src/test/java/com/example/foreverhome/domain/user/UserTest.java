package com.example.foreverhome.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User entity")
class UserTest {

    @Nested
    @DisplayName("when creating a new user")
    class WhenCreatingNewUser {

        @Test
        @DisplayName("should create user with required fields")
        void shouldCreateUserWithRequiredFields() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.getId()).isNotNull();
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getPasswordHash()).isEqualTo("hashedPassword123");
            assertThat(user.getRole()).isEqualTo(UserRole.FOSTER);
        }

        @Test
        @DisplayName("should set status to PENDING for new users")
        void shouldSetStatusToPendingForNewUsers() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING);
        }

        @Test
        @DisplayName("should set profileComplete to false for new users")
        void shouldSetProfileCompleteToFalseForNewUsers() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.isProfileComplete()).isFalse();
        }

        @Test
        @DisplayName("should set default notification preferences")
        void shouldSetDefaultNotificationPreferences() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.getNotificationPreferences()).isEqualTo(NotificationPreferences.defaults());
        }

        @Test
        @DisplayName("should set createdAt timestamp")
        void shouldSetCreatedAtTimestamp() {
            Instant before = Instant.now();
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            Instant after = Instant.now();

            assertThat(user.getCreatedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should not allow null email")
        void shouldNotAllowNullEmail() {
            assertThatThrownBy(() -> User.create(null, "hashedPassword123", UserRole.FOSTER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("should not allow blank email")
        void shouldNotAllowBlankEmail() {
            assertThatThrownBy(() -> User.create("  ", "hashedPassword123", UserRole.FOSTER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("should not allow null password hash")
        void shouldNotAllowNullPasswordHash() {
            assertThatThrownBy(() -> User.create("test@example.com", null, UserRole.FOSTER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("should not allow null role")
        void shouldNotAllowNullRole() {
            assertThatThrownBy(() -> User.create("test@example.com", "hashedPassword123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role");
        }
    }

    @Nested
    @DisplayName("when activating a user")
    class WhenActivatingUser {

        @Test
        @DisplayName("should change status from PENDING to ACTIVE")
        void shouldChangeStatusFromPendingToActive() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            user.activate();

            assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw exception if already active")
        void shouldThrowExceptionIfAlreadyActive() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();

            assertThatThrownBy(user::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot activate");
        }
    }

    @Nested
    @DisplayName("when suspending a user")
    class WhenSuspendingUser {

        @Test
        @DisplayName("should change status from ACTIVE to SUSPENDED")
        void shouldChangeStatusFromActiveToSuspended() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();

            user.suspend();

            assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should throw exception if not active")
        void shouldThrowExceptionIfNotActive() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThatThrownBy(user::suspend)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot suspend");
        }
    }

    @Nested
    @DisplayName("when reactivating a suspended user")
    class WhenReactivatingSuspendedUser {

        @Test
        @DisplayName("should change status from SUSPENDED to ACTIVE")
        void shouldChangeStatusFromSuspendedToActive() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();
            user.suspend();

            user.reactivate();

            assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("when completing profile")
    class WhenCompletingProfile {

        @Test
        @DisplayName("should set profileComplete to true")
        void shouldSetProfileCompleteToTrue() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            user.markProfileComplete();

            assertThat(user.isProfileComplete()).isTrue();
        }
    }

    @Nested
    @DisplayName("when recording login")
    class WhenRecordingLogin {

        @Test
        @DisplayName("should update lastLoginAt timestamp")
        void shouldUpdateLastLoginAtTimestamp() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();

            Instant before = Instant.now();
            user.recordLogin();
            Instant after = Instant.now();

            assertThat(user.getLastLoginAt())
                    .isNotNull()
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should throw exception if user cannot login")
        void shouldThrowExceptionIfUserCannotLogin() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            // User is PENDING, cannot login

            assertThatThrownBy(user::recordLogin)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot login");
        }
    }

    @Nested
    @DisplayName("when checking login eligibility")
    class WhenCheckingLoginEligibility {

        @Test
        @DisplayName("active user should be able to login")
        void activeUserShouldBeAbleToLogin() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();

            assertThat(user.canLogin()).isTrue();
        }

        @Test
        @DisplayName("pending user should not be able to login")
        void pendingUserShouldNotBeAbleToLogin() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.canLogin()).isFalse();
        }

        @Test
        @DisplayName("suspended user should not be able to login")
        void suspendedUserShouldNotBeAbleToLogin() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            user.activate();
            user.suspend();

            assertThat(user.canLogin()).isFalse();
        }
    }

    @Nested
    @DisplayName("when updating notification preferences")
    class WhenUpdatingNotificationPreferences {

        @Test
        @DisplayName("should update preferences")
        void shouldUpdatePreferences() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);
            NotificationPreferences newPrefs = new NotificationPreferences(false, true, false, true);

            user.updateNotificationPreferences(newPrefs);

            assertThat(user.getNotificationPreferences()).isEqualTo(newPrefs);
        }

        @Test
        @DisplayName("should not allow null preferences")
        void shouldNotAllowNullPreferences() {
            User user = User.create("test@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThatThrownBy(() -> user.updateNotificationPreferences(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("when checking if user requires verification")
    class WhenCheckingIfUserRequiresVerification {

        @Test
        @DisplayName("VET role should require verification")
        void vetRoleShouldRequireVerification() {
            User user = User.create("vet@example.com", "hashedPassword123", UserRole.VET);

            assertThat(user.requiresVerification()).isTrue();
        }

        @Test
        @DisplayName("RESCUE_ORG role should require verification")
        void rescueOrgRoleShouldRequireVerification() {
            User user = User.create("rescue@example.com", "hashedPassword123", UserRole.RESCUE_ORG);

            assertThat(user.requiresVerification()).isTrue();
        }

        @Test
        @DisplayName("FOSTER role should not require verification")
        void fosterRoleShouldNotRequireVerification() {
            User user = User.create("foster@example.com", "hashedPassword123", UserRole.FOSTER);

            assertThat(user.requiresVerification()).isFalse();
        }
    }

    @Nested
    @DisplayName("when updating password")
    class WhenUpdatingPassword {

        @Test
        @DisplayName("should update password hash")
        void shouldUpdatePasswordHash() {
            User user = User.create("test@example.com", "oldHash", UserRole.FOSTER);

            user.updatePasswordHash("newHash");

            assertThat(user.getPasswordHash()).isEqualTo("newHash");
        }

        @Test
        @DisplayName("should not allow null password hash")
        void shouldNotAllowNullPasswordHash() {
            User user = User.create("test@example.com", "oldHash", UserRole.FOSTER);

            assertThatThrownBy(() -> user.updatePasswordHash(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
