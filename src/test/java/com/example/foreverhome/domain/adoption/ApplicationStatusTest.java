package com.example.foreverhome.domain.adoption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationStatus enum")
class ApplicationStatusTest {

    @Nested
    @DisplayName("given all defined application statuses")
    class GivenAllDefinedStatuses {

        @Test
        @DisplayName("should have exactly 6 statuses")
        void shouldHaveExactlySixStatuses() {
            assertThat(ApplicationStatus.values())
                    .hasSize(6)
                    .containsExactlyInAnyOrder(
                            ApplicationStatus.SUBMITTED,
                            ApplicationStatus.UNDER_REVIEW,
                            ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.FINALIZED
                    );
        }
    }

    @Nested
    @DisplayName("when checking if application is active")
    class WhenCheckingIfApplicationIsActive {

        @Test
        @DisplayName("SUBMITTED status should be active")
        void submittedShouldBeActive() {
            assertThat(ApplicationStatus.SUBMITTED.isActive()).isTrue();
        }

        @Test
        @DisplayName("UNDER_REVIEW status should be active")
        void underReviewShouldBeActive() {
            assertThat(ApplicationStatus.UNDER_REVIEW.isActive()).isTrue();
        }

        @Test
        @DisplayName("APPROVED status should not be active")
        void approvedShouldNotBeActive() {
            assertThat(ApplicationStatus.APPROVED.isActive()).isFalse();
        }

        @Test
        @DisplayName("REJECTED status should not be active")
        void rejectedShouldNotBeActive() {
            assertThat(ApplicationStatus.REJECTED.isActive()).isFalse();
        }

        @Test
        @DisplayName("WITHDRAWN status should not be active")
        void withdrawnShouldNotBeActive() {
            assertThat(ApplicationStatus.WITHDRAWN.isActive()).isFalse();
        }

        @Test
        @DisplayName("FINALIZED status should not be active")
        void finalizedShouldNotBeActive() {
            assertThat(ApplicationStatus.FINALIZED.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions")
    class WhenCheckingValidTransitions {

        @Test
        @DisplayName("SUBMITTED can transition to UNDER_REVIEW")
        void submittedCanTransitionToUnderReview() {
            assertThat(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isTrue();
        }

        @Test
        @DisplayName("SUBMITTED can transition to WITHDRAWN")
        void submittedCanTransitionToWithdrawn() {
            assertThat(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.WITHDRAWN)).isTrue();
        }

        @Test
        @DisplayName("UNDER_REVIEW can transition to APPROVED")
        void underReviewCanTransitionToApproved() {
            assertThat(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.APPROVED)).isTrue();
        }

        @Test
        @DisplayName("UNDER_REVIEW can transition to REJECTED")
        void underReviewCanTransitionToRejected() {
            assertThat(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.REJECTED)).isTrue();
        }

        @Test
        @DisplayName("APPROVED can only transition to FINALIZED")
        void approvedCanOnlyTransitionToFinalized() {
            assertThat(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.FINALIZED)).isTrue();
            assertThat(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.SUBMITTED)).isFalse();
            assertThat(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isFalse();
            assertThat(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.REJECTED)).isFalse();
            assertThat(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.WITHDRAWN)).isFalse();
        }

        @Test
        @DisplayName("FINALIZED cannot transition to any other status")
        void finalizedCannotTransition() {
            assertThat(ApplicationStatus.FINALIZED.canTransitionTo(ApplicationStatus.SUBMITTED)).isFalse();
            assertThat(ApplicationStatus.FINALIZED.canTransitionTo(ApplicationStatus.UNDER_REVIEW)).isFalse();
            assertThat(ApplicationStatus.FINALIZED.canTransitionTo(ApplicationStatus.APPROVED)).isFalse();
            assertThat(ApplicationStatus.FINALIZED.canTransitionTo(ApplicationStatus.REJECTED)).isFalse();
            assertThat(ApplicationStatus.FINALIZED.canTransitionTo(ApplicationStatus.WITHDRAWN)).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking if withdrawal is allowed")
    class WhenCheckingIfWithdrawalAllowed {

        @Test
        @DisplayName("SUBMITTED status allows withdrawal")
        void submittedAllowsWithdrawal() {
            assertThat(ApplicationStatus.SUBMITTED.canWithdraw()).isTrue();
        }

        @Test
        @DisplayName("UNDER_REVIEW status allows withdrawal")
        void underReviewAllowsWithdrawal() {
            assertThat(ApplicationStatus.UNDER_REVIEW.canWithdraw()).isTrue();
        }

        @Test
        @DisplayName("APPROVED status does not allow withdrawal")
        void approvedDoesNotAllowWithdrawal() {
            assertThat(ApplicationStatus.APPROVED.canWithdraw()).isFalse();
        }

        @Test
        @DisplayName("FINALIZED status does not allow withdrawal")
        void finalizedDoesNotAllowWithdrawal() {
            assertThat(ApplicationStatus.FINALIZED.canWithdraw()).isFalse();
        }
    }
}
