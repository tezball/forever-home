package com.example.foreverhome.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationPreferences value object")
class NotificationPreferencesTest {

    @Nested
    @DisplayName("given default preferences")
    class GivenDefaultPreferences {

        @Test
        @DisplayName("defaults() should enable all notifications")
        void defaultsShouldEnableAllNotifications() {
            NotificationPreferences prefs = NotificationPreferences.defaults();

            assertThat(prefs.emailStatusChanges()).isTrue();
            assertThat(prefs.emailNewApplications()).isTrue();
            assertThat(prefs.emailFavoriteUpdates()).isTrue();
            assertThat(prefs.inAppEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("given custom preferences")
    class GivenCustomPreferences {

        @Test
        @DisplayName("should store all preference values")
        void shouldStoreAllPreferenceValues() {
            NotificationPreferences prefs = new NotificationPreferences(
                    true,   // emailStatusChanges
                    false,  // emailNewApplications
                    true,   // emailFavoriteUpdates
                    false   // inAppEnabled
            );

            assertThat(prefs.emailStatusChanges()).isTrue();
            assertThat(prefs.emailNewApplications()).isFalse();
            assertThat(prefs.emailFavoriteUpdates()).isTrue();
            assertThat(prefs.inAppEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking if any email notifications are enabled")
    class WhenCheckingIfAnyEmailEnabled {

        @Test
        @DisplayName("should return true if any email preference is enabled")
        void shouldReturnTrueIfAnyEmailPreferenceEnabled() {
            NotificationPreferences prefs = new NotificationPreferences(false, false, true, false);

            assertThat(prefs.hasAnyEmailEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return false if no email preferences are enabled")
        void shouldReturnFalseIfNoEmailPreferencesEnabled() {
            NotificationPreferences prefs = new NotificationPreferences(false, false, false, true);

            assertThat(prefs.hasAnyEmailEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("when modifying preferences")
    class WhenModifyingPreferences {

        @Test
        @DisplayName("withEmailStatusChanges should return new instance with updated value")
        void withEmailStatusChangesShouldReturnNewInstanceWithUpdatedValue() {
            NotificationPreferences original = NotificationPreferences.defaults();
            NotificationPreferences updated = original.withEmailStatusChanges(false);

            assertThat(updated.emailStatusChanges()).isFalse();
            assertThat(updated.emailNewApplications()).isTrue(); // unchanged
            assertThat(original.emailStatusChanges()).isTrue(); // original unchanged
        }

        @Test
        @DisplayName("disableAllEmails should disable all email notifications")
        void disableAllEmailsShouldDisableAllEmailNotifications() {
            NotificationPreferences prefs = NotificationPreferences.defaults().disableAllEmails();

            assertThat(prefs.emailStatusChanges()).isFalse();
            assertThat(prefs.emailNewApplications()).isFalse();
            assertThat(prefs.emailFavoriteUpdates()).isFalse();
            assertThat(prefs.inAppEnabled()).isTrue(); // in-app unchanged
        }
    }

    @Nested
    @DisplayName("when checking equality")
    class WhenCheckingEquality {

        @Test
        @DisplayName("two preferences with same values should be equal")
        void twoPreferencesWithSameValuesShouldBeEqual() {
            NotificationPreferences prefs1 = new NotificationPreferences(true, true, false, true);
            NotificationPreferences prefs2 = new NotificationPreferences(true, true, false, true);

            assertThat(prefs1).isEqualTo(prefs2);
            assertThat(prefs1.hashCode()).isEqualTo(prefs2.hashCode());
        }
    }
}
