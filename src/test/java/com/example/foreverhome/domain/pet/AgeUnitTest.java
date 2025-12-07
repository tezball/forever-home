package com.example.foreverhome.domain.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgeUnit enum")
class AgeUnitTest {

    @Nested
    @DisplayName("given all defined age units")
    class GivenAllDefinedAgeUnits {

        @Test
        @DisplayName("should have exactly 2 units: MONTHS, YEARS")
        void shouldHaveExactlyTwoUnits() {
            assertThat(AgeUnit.values())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            AgeUnit.MONTHS,
                            AgeUnit.YEARS
                    );
        }
    }

    @Nested
    @DisplayName("when formatting age display")
    class WhenFormattingAgeDisplay {

        @Test
        @DisplayName("1 month should display as '1 month'")
        void oneMonthShouldDisplaySingular() {
            assertThat(AgeUnit.MONTHS.formatAge(1)).isEqualTo("1 month");
        }

        @Test
        @DisplayName("5 months should display as '5 months'")
        void fiveMonthsShouldDisplayPlural() {
            assertThat(AgeUnit.MONTHS.formatAge(5)).isEqualTo("5 months");
        }

        @Test
        @DisplayName("1 year should display as '1 year'")
        void oneYearShouldDisplaySingular() {
            assertThat(AgeUnit.YEARS.formatAge(1)).isEqualTo("1 year");
        }

        @Test
        @DisplayName("3 years should display as '3 years'")
        void threeYearsShouldDisplayPlural() {
            assertThat(AgeUnit.YEARS.formatAge(3)).isEqualTo("3 years");
        }
    }
}
