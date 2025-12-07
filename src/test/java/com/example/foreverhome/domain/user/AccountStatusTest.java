package com.example.foreverhome.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountStatus enum")
class AccountStatusTest {

    @Nested
    @DisplayName("given all defined statuses")
    class GivenAllDefinedStatuses {

        @Test
        @DisplayName("should have exactly 3 statuses: PENDING, ACTIVE, SUSPENDED")
        void shouldHaveExactlyThreeStatuses() {
            assertThat(AccountStatus.values())
                    .hasSize(3)
                    .containsExactlyInAnyOrder(
                            AccountStatus.PENDING,
                            AccountStatus.ACTIVE,
                            AccountStatus.SUSPENDED
                    );
        }
    }

    @Nested
    @DisplayName("when checking if account can perform actions")
    class WhenCheckingIfAccountCanPerformActions {

        @Test
        @DisplayName("ACTIVE status should allow login")
        void activeStatusShouldAllowLogin() {
            assertThat(AccountStatus.ACTIVE.canLogin()).isTrue();
        }

        @Test
        @DisplayName("PENDING status should not allow login")
        void pendingStatusShouldNotAllowLogin() {
            assertThat(AccountStatus.PENDING.canLogin()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED status should not allow login")
        void suspendedStatusShouldNotAllowLogin() {
            assertThat(AccountStatus.SUSPENDED.canLogin()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking if account can be activated")
    class WhenCheckingIfAccountCanBeActivated {

        @Test
        @DisplayName("PENDING status can transition to ACTIVE")
        void pendingCanTransitionToActive() {
            assertThat(AccountStatus.PENDING.canTransitionTo(AccountStatus.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE status can transition to SUSPENDED")
        void activeCanTransitionToSuspended() {
            assertThat(AccountStatus.ACTIVE.canTransitionTo(AccountStatus.SUSPENDED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED status can transition to ACTIVE")
        void suspendedCanTransitionToActive() {
            assertThat(AccountStatus.SUSPENDED.canTransitionTo(AccountStatus.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE status cannot transition to PENDING")
        void activeCannotTransitionToPending() {
            assertThat(AccountStatus.ACTIVE.canTransitionTo(AccountStatus.PENDING)).isFalse();
        }
    }
}
