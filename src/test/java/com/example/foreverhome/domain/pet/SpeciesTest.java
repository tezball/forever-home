package com.example.foreverhome.domain.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Species enum")
class SpeciesTest {

    @Nested
    @DisplayName("given all defined species")
    class GivenAllDefinedSpecies {

        @Test
        @DisplayName("should have exactly 5 species: DOG, CAT, RABBIT, BIRD, OTHER")
        void shouldHaveExactlyFiveSpecies() {
            assertThat(Species.values())
                    .hasSize(5)
                    .containsExactlyInAnyOrder(
                            Species.DOG,
                            Species.CAT,
                            Species.RABBIT,
                            Species.BIRD,
                            Species.OTHER
                    );
        }
    }

    @Nested
    @DisplayName("when getting display name")
    class WhenGettingDisplayName {

        @Test
        @DisplayName("DOG should have display name 'Dog'")
        void dogShouldHaveCorrectDisplayName() {
            assertThat(Species.DOG.getDisplayName()).isEqualTo("Dog");
        }

        @Test
        @DisplayName("CAT should have display name 'Cat'")
        void catShouldHaveCorrectDisplayName() {
            assertThat(Species.CAT.getDisplayName()).isEqualTo("Cat");
        }

        @Test
        @DisplayName("RABBIT should have display name 'Rabbit'")
        void rabbitShouldHaveCorrectDisplayName() {
            assertThat(Species.RABBIT.getDisplayName()).isEqualTo("Rabbit");
        }

        @Test
        @DisplayName("BIRD should have display name 'Bird'")
        void birdShouldHaveCorrectDisplayName() {
            assertThat(Species.BIRD.getDisplayName()).isEqualTo("Bird");
        }

        @Test
        @DisplayName("OTHER should have display name 'Other'")
        void otherShouldHaveCorrectDisplayName() {
            assertThat(Species.OTHER.getDisplayName()).isEqualTo("Other");
        }
    }
}
