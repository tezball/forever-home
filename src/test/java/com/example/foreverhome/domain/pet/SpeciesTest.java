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
        @DisplayName("should have exactly 2 species: DOG, CAT")
        void shouldHaveExactlyTwoSpecies() {
            assertThat(Species.values())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            Species.DOG,
                            Species.CAT
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
    }
}
