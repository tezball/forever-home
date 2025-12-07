package com.example.foreverhome.domain.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PetSize enum")
class PetSizeTest {

    @Nested
    @DisplayName("given all defined sizes")
    class GivenAllDefinedSizes {

        @Test
        @DisplayName("should have exactly 3 sizes: SMALL, MEDIUM, LARGE")
        void shouldHaveExactlyThreeSizes() {
            assertThat(PetSize.values())
                    .hasSize(3)
                    .containsExactlyInAnyOrder(
                            PetSize.SMALL,
                            PetSize.MEDIUM,
                            PetSize.LARGE
                    );
        }
    }

    @Nested
    @DisplayName("when getting display name")
    class WhenGettingDisplayName {

        @Test
        @DisplayName("SMALL should have display name 'Small'")
        void smallShouldHaveCorrectDisplayName() {
            assertThat(PetSize.SMALL.getDisplayName()).isEqualTo("Small");
        }

        @Test
        @DisplayName("MEDIUM should have display name 'Medium'")
        void mediumShouldHaveCorrectDisplayName() {
            assertThat(PetSize.MEDIUM.getDisplayName()).isEqualTo("Medium");
        }

        @Test
        @DisplayName("LARGE should have display name 'Large'")
        void largeShouldHaveCorrectDisplayName() {
            assertThat(PetSize.LARGE.getDisplayName()).isEqualTo("Large");
        }
    }
}
