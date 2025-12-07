package com.example.foreverhome.domain.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PetSex enum")
class PetSexTest {

    @Nested
    @DisplayName("given all defined sexes")
    class GivenAllDefinedSexes {

        @Test
        @DisplayName("should have exactly 2 values: MALE, FEMALE")
        void shouldHaveExactlyTwoValues() {
            assertThat(PetSex.values())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            PetSex.MALE,
                            PetSex.FEMALE
                    );
        }
    }

    @Nested
    @DisplayName("when getting display name")
    class WhenGettingDisplayName {

        @Test
        @DisplayName("MALE should have display name 'Male'")
        void maleShouldHaveCorrectDisplayName() {
            assertThat(PetSex.MALE.getDisplayName()).isEqualTo("Male");
        }

        @Test
        @DisplayName("FEMALE should have display name 'Female'")
        void femaleShouldHaveCorrectDisplayName() {
            assertThat(PetSex.FEMALE.getDisplayName()).isEqualTo("Female");
        }
    }
}
