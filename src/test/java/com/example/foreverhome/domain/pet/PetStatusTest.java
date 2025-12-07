package com.example.foreverhome.domain.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PetStatus enum")
class PetStatusTest {

    @Nested
    @DisplayName("given all defined statuses")
    class GivenAllDefinedStatuses {

        @Test
        @DisplayName("should have exactly 8 statuses")
        void shouldHaveExactlyEightStatuses() {
            assertThat(PetStatus.values())
                    .hasSize(8)
                    .containsExactlyInAnyOrder(
                            PetStatus.DRAFT,
                            PetStatus.PENDING_RESCUE,
                            PetStatus.PENDING_VET,
                            PetStatus.AVAILABLE,
                            PetStatus.IN_PROGRESS,
                            PetStatus.ADOPTED,
                            PetStatus.WITHDRAWN,
                            PetStatus.ON_HOLD
                    );
        }
    }

    @Nested
    @DisplayName("when checking public visibility")
    class WhenCheckingPublicVisibility {

        @Test
        @DisplayName("AVAILABLE status should be publicly visible")
        void availableShouldBePubliclyVisible() {
            assertThat(PetStatus.AVAILABLE.isPubliclyVisible()).isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS status should be publicly visible")
        void inProgressShouldBePubliclyVisible() {
            assertThat(PetStatus.IN_PROGRESS.isPubliclyVisible()).isTrue();
        }

        @Test
        @DisplayName("ON_HOLD status should be publicly visible")
        void onHoldShouldBePubliclyVisible() {
            assertThat(PetStatus.ON_HOLD.isPubliclyVisible()).isTrue();
        }

        @Test
        @DisplayName("DRAFT status should not be publicly visible")
        void draftShouldNotBePubliclyVisible() {
            assertThat(PetStatus.DRAFT.isPubliclyVisible()).isFalse();
        }

        @Test
        @DisplayName("PENDING_RESCUE status should not be publicly visible")
        void pendingRescueShouldNotBePubliclyVisible() {
            assertThat(PetStatus.PENDING_RESCUE.isPubliclyVisible()).isFalse();
        }

        @Test
        @DisplayName("PENDING_VET status should not be publicly visible")
        void pendingVetShouldNotBePubliclyVisible() {
            assertThat(PetStatus.PENDING_VET.isPubliclyVisible()).isFalse();
        }

        @Test
        @DisplayName("ADOPTED status should not be publicly visible")
        void adoptedShouldNotBePubliclyVisible() {
            assertThat(PetStatus.ADOPTED.isPubliclyVisible()).isFalse();
        }

        @Test
        @DisplayName("WITHDRAWN status should not be publicly visible")
        void withdrawnShouldNotBePubliclyVisible() {
            assertThat(PetStatus.WITHDRAWN.isPubliclyVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions from DRAFT")
    class WhenCheckingTransitionsFromDraft {

        @Test
        @DisplayName("DRAFT can transition to PENDING_RESCUE (foster submits)")
        void draftCanTransitionToPendingRescue() {
            assertThat(PetStatus.DRAFT.canTransitionTo(PetStatus.PENDING_RESCUE)).isTrue();
        }

        @Test
        @DisplayName("DRAFT can transition to WITHDRAWN (foster withdraws)")
        void draftCanTransitionToWithdrawn() {
            assertThat(PetStatus.DRAFT.canTransitionTo(PetStatus.WITHDRAWN)).isTrue();
        }

        @Test
        @DisplayName("DRAFT cannot transition directly to AVAILABLE")
        void draftCannotTransitionToAvailable() {
            assertThat(PetStatus.DRAFT.canTransitionTo(PetStatus.AVAILABLE)).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions from PENDING_RESCUE")
    class WhenCheckingTransitionsFromPendingRescue {

        @Test
        @DisplayName("PENDING_RESCUE can transition to PENDING_VET (rescue accepts)")
        void pendingRescueCanTransitionToPendingVet() {
            assertThat(PetStatus.PENDING_RESCUE.canTransitionTo(PetStatus.PENDING_VET)).isTrue();
        }

        @Test
        @DisplayName("PENDING_RESCUE can transition to DRAFT (rescue declines)")
        void pendingRescueCanTransitionToDraft() {
            assertThat(PetStatus.PENDING_RESCUE.canTransitionTo(PetStatus.DRAFT)).isTrue();
        }

        @Test
        @DisplayName("PENDING_RESCUE can transition to WITHDRAWN (foster withdraws)")
        void pendingRescueCanTransitionToWithdrawn() {
            assertThat(PetStatus.PENDING_RESCUE.canTransitionTo(PetStatus.WITHDRAWN)).isTrue();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions from PENDING_VET")
    class WhenCheckingTransitionsFromPendingVet {

        @Test
        @DisplayName("PENDING_VET can transition to AVAILABLE (vet signs off)")
        void pendingVetCanTransitionToAvailable() {
            assertThat(PetStatus.PENDING_VET.canTransitionTo(PetStatus.AVAILABLE)).isTrue();
        }

        @Test
        @DisplayName("PENDING_VET can transition to PENDING_RESCUE (vet declines)")
        void pendingVetCanTransitionToPendingRescue() {
            assertThat(PetStatus.PENDING_VET.canTransitionTo(PetStatus.PENDING_RESCUE)).isTrue();
        }

        @Test
        @DisplayName("PENDING_VET can transition to WITHDRAWN (foster withdraws)")
        void pendingVetCanTransitionToWithdrawn() {
            assertThat(PetStatus.PENDING_VET.canTransitionTo(PetStatus.WITHDRAWN)).isTrue();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions from AVAILABLE")
    class WhenCheckingTransitionsFromAvailable {

        @Test
        @DisplayName("AVAILABLE can transition to IN_PROGRESS (application approved)")
        void availableCanTransitionToInProgress() {
            assertThat(PetStatus.AVAILABLE.canTransitionTo(PetStatus.IN_PROGRESS)).isTrue();
        }

        @Test
        @DisplayName("AVAILABLE can transition to ON_HOLD")
        void availableCanTransitionToOnHold() {
            assertThat(PetStatus.AVAILABLE.canTransitionTo(PetStatus.ON_HOLD)).isTrue();
        }

        @Test
        @DisplayName("AVAILABLE can transition to WITHDRAWN (foster withdraws)")
        void availableCanTransitionToWithdrawn() {
            assertThat(PetStatus.AVAILABLE.canTransitionTo(PetStatus.WITHDRAWN)).isTrue();
        }
    }

    @Nested
    @DisplayName("when checking valid transitions from IN_PROGRESS")
    class WhenCheckingTransitionsFromInProgress {

        @Test
        @DisplayName("IN_PROGRESS can transition to ADOPTED (adoption finalized)")
        void inProgressCanTransitionToAdopted() {
            assertThat(PetStatus.IN_PROGRESS.canTransitionTo(PetStatus.ADOPTED)).isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS can transition to AVAILABLE (adoption cancelled)")
        void inProgressCanTransitionToAvailable() {
            assertThat(PetStatus.IN_PROGRESS.canTransitionTo(PetStatus.AVAILABLE)).isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS can transition to WITHDRAWN (foster withdraws with rescue approval)")
        void inProgressCanTransitionToWithdrawn() {
            assertThat(PetStatus.IN_PROGRESS.canTransitionTo(PetStatus.WITHDRAWN)).isTrue();
        }
    }

    @Nested
    @DisplayName("when checking terminal statuses")
    class WhenCheckingTerminalStatuses {

        @Test
        @DisplayName("ADOPTED is a terminal status")
        void adoptedIsTerminal() {
            assertThat(PetStatus.ADOPTED.isTerminal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = PetStatus.class, names = {"ADOPTED"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("non-terminal statuses should not be terminal")
        void nonTerminalStatusesShouldNotBeTerminal(PetStatus status) {
            assertThat(status.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking if pet is editable")
    class WhenCheckingIfPetIsEditable {

        @Test
        @DisplayName("DRAFT status allows editing")
        void draftAllowsEditing() {
            assertThat(PetStatus.DRAFT.isEditable()).isTrue();
        }

        @Test
        @DisplayName("PENDING_RESCUE status allows editing")
        void pendingRescueAllowsEditing() {
            assertThat(PetStatus.PENDING_RESCUE.isEditable()).isTrue();
        }

        @Test
        @DisplayName("PENDING_VET status allows editing")
        void pendingVetAllowsEditing() {
            assertThat(PetStatus.PENDING_VET.isEditable()).isTrue();
        }

        @Test
        @DisplayName("AVAILABLE status allows editing")
        void availableAllowsEditing() {
            assertThat(PetStatus.AVAILABLE.isEditable()).isTrue();
        }

        @Test
        @DisplayName("IN_PROGRESS status does not allow editing")
        void inProgressDoesNotAllowEditing() {
            assertThat(PetStatus.IN_PROGRESS.isEditable()).isFalse();
        }

        @Test
        @DisplayName("ADOPTED status does not allow editing")
        void adoptedDoesNotAllowEditing() {
            assertThat(PetStatus.ADOPTED.isEditable()).isFalse();
        }
    }
}
