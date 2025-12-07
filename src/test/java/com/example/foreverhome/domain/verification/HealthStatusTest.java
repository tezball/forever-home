package com.example.foreverhome.domain.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthStatus enum")
class HealthStatusTest {

    @Nested
    @DisplayName("given all defined health statuses")
    class GivenAllDefinedHealthStatuses {

        @Test
        @DisplayName("should have exactly 2 statuses: GOOD, KNOWN_CONDITIONS")
        void shouldHaveExactlyTwoStatuses() {
            assertThat(HealthStatus.values())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            HealthStatus.GOOD,
                            HealthStatus.KNOWN_CONDITIONS
                    );
        }
    }

    @Nested
    @DisplayName("when checking if notes are required")
    class WhenCheckingIfNotesRequired {

        @Test
        @DisplayName("GOOD status should not require health notes")
        void goodStatusShouldNotRequireNotes() {
            assertThat(HealthStatus.GOOD.requiresHealthNotes()).isFalse();
        }

        @Test
        @DisplayName("KNOWN_CONDITIONS status should require health notes")
        void knownConditionsShouldRequireNotes() {
            assertThat(HealthStatus.KNOWN_CONDITIONS.requiresHealthNotes()).isTrue();
        }
    }

    @Nested
    @DisplayName("when getting display name")
    class WhenGettingDisplayName {

        @Test
        @DisplayName("GOOD should have display name 'Good'")
        void goodShouldHaveCorrectDisplayName() {
            assertThat(HealthStatus.GOOD.getDisplayName()).isEqualTo("Good");
        }

        @Test
        @DisplayName("KNOWN_CONDITIONS should have display name 'Known Conditions'")
        void knownConditionsShouldHaveCorrectDisplayName() {
            assertThat(HealthStatus.KNOWN_CONDITIONS.getDisplayName()).isEqualTo("Known Conditions");
        }
    }
}
