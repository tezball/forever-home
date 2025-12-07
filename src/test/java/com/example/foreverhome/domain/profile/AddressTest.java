package com.example.foreverhome.domain.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address value object")
class AddressTest {

    @Nested
    @DisplayName("given a complete address")
    class GivenCompleteAddress {

        @Test
        @DisplayName("should store all address components")
        void shouldStoreAllAddressComponents() {
            Address address = new Address(
                    "123 Main St",
                    "Springfield",
                    "IL",
                    "62701",
                    "USA"
            );

            assertThat(address.street()).isEqualTo("123 Main St");
            assertThat(address.city()).isEqualTo("Springfield");
            assertThat(address.state()).isEqualTo("IL");
            assertThat(address.postalCode()).isEqualTo("62701");
            assertThat(address.country()).isEqualTo("USA");
        }

        @Test
        @DisplayName("should format as single line string")
        void shouldFormatAsSingleLineString() {
            Address address = new Address(
                    "123 Main St",
                    "Springfield",
                    "IL",
                    "62701",
                    "USA"
            );

            assertThat(address.toSingleLine())
                    .isEqualTo("123 Main St, Springfield, IL 62701, USA");
        }

        @Test
        @DisplayName("should format city and state for display")
        void shouldFormatCityAndStateForDisplay() {
            Address address = new Address(
                    "123 Main St",
                    "Springfield",
                    "IL",
                    "62701",
                    "USA"
            );

            assertThat(address.getCityState()).isEqualTo("Springfield, IL");
        }
    }

    @Nested
    @DisplayName("given an address with optional fields null")
    class GivenAddressWithNullFields {

        @Test
        @DisplayName("should allow null street for privacy")
        void shouldAllowNullStreet() {
            Address address = new Address(
                    null,
                    "Springfield",
                    "IL",
                    "62701",
                    "USA"
            );

            assertThat(address.street()).isNull();
            assertThat(address.city()).isEqualTo("Springfield");
        }

        @Test
        @DisplayName("should format single line without null street")
        void shouldFormatSingleLineWithoutNullStreet() {
            Address address = new Address(
                    null,
                    "Springfield",
                    "IL",
                    "62701",
                    "USA"
            );

            assertThat(address.toSingleLine())
                    .isEqualTo("Springfield, IL 62701, USA");
        }
    }

    @Nested
    @DisplayName("when checking equality")
    class WhenCheckingEquality {

        @Test
        @DisplayName("two addresses with same values should be equal")
        void twoAddressesWithSameValuesShouldBeEqual() {
            Address address1 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
            Address address2 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

            assertThat(address1).isEqualTo(address2);
            assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
        }

        @Test
        @DisplayName("two addresses with different values should not be equal")
        void twoAddressesWithDifferentValuesShouldNotBeEqual() {
            Address address1 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
            Address address2 = new Address("456 Oak Ave", "Springfield", "IL", "62701", "USA");

            assertThat(address1).isNotEqualTo(address2);
        }
    }
}
