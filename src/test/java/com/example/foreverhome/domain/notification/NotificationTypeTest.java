package com.example.foreverhome.domain.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationType enum")
class NotificationTypeTest {

    @Nested
    @DisplayName("given all defined notification types")
    class GivenAllDefinedTypes {

        @Test
        @DisplayName("should have exactly 5 types")
        void shouldHaveExactlyFiveTypes() {
            assertThat(NotificationType.values())
                    .hasSize(5)
                    .containsExactlyInAnyOrder(
                            NotificationType.PET_STATUS_CHANGE,
                            NotificationType.NEW_APPLICATION,
                            NotificationType.APPLICATION_UPDATE,
                            NotificationType.FAVORITE_UPDATE,
                            NotificationType.SYSTEM_ALERT
                    );
        }
    }

    @Nested
    @DisplayName("when getting display name")
    class WhenGettingDisplayName {

        @Test
        @DisplayName("PET_STATUS_CHANGE should have display name 'Pet Status Change'")
        void petStatusChangeShouldHaveCorrectDisplayName() {
            assertThat(NotificationType.PET_STATUS_CHANGE.getDisplayName()).isEqualTo("Pet Status Change");
        }

        @Test
        @DisplayName("NEW_APPLICATION should have display name 'New Application'")
        void newApplicationShouldHaveCorrectDisplayName() {
            assertThat(NotificationType.NEW_APPLICATION.getDisplayName()).isEqualTo("New Application");
        }

        @Test
        @DisplayName("APPLICATION_UPDATE should have display name 'Application Update'")
        void applicationUpdateShouldHaveCorrectDisplayName() {
            assertThat(NotificationType.APPLICATION_UPDATE.getDisplayName()).isEqualTo("Application Update");
        }

        @Test
        @DisplayName("FAVORITE_UPDATE should have display name 'Favorite Update'")
        void favoriteUpdateShouldHaveCorrectDisplayName() {
            assertThat(NotificationType.FAVORITE_UPDATE.getDisplayName()).isEqualTo("Favorite Update");
        }

        @Test
        @DisplayName("SYSTEM_ALERT should have display name 'System Alert'")
        void systemAlertShouldHaveCorrectDisplayName() {
            assertThat(NotificationType.SYSTEM_ALERT.getDisplayName()).isEqualTo("System Alert");
        }
    }

    @Nested
    @DisplayName("when checking email eligibility")
    class WhenCheckingEmailEligibility {

        @Test
        @DisplayName("PET_STATUS_CHANGE should be email eligible")
        void petStatusChangeShouldBeEmailEligible() {
            assertThat(NotificationType.PET_STATUS_CHANGE.isEmailEligible()).isTrue();
        }

        @Test
        @DisplayName("NEW_APPLICATION should be email eligible")
        void newApplicationShouldBeEmailEligible() {
            assertThat(NotificationType.NEW_APPLICATION.isEmailEligible()).isTrue();
        }

        @Test
        @DisplayName("APPLICATION_UPDATE should be email eligible")
        void applicationUpdateShouldBeEmailEligible() {
            assertThat(NotificationType.APPLICATION_UPDATE.isEmailEligible()).isTrue();
        }

        @Test
        @DisplayName("FAVORITE_UPDATE should be email eligible")
        void favoriteUpdateShouldBeEmailEligible() {
            assertThat(NotificationType.FAVORITE_UPDATE.isEmailEligible()).isTrue();
        }

        @Test
        @DisplayName("SYSTEM_ALERT should be email eligible")
        void systemAlertShouldBeEmailEligible() {
            assertThat(NotificationType.SYSTEM_ALERT.isEmailEligible()).isTrue();
        }
    }
}
