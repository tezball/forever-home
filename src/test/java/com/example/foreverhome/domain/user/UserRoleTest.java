package com.example.foreverhome.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRole enum")
class UserRoleTest {

    @Nested
    @DisplayName("given all defined roles")
    class GivenAllDefinedRoles {

        @Test
        @DisplayName("should have exactly 5 roles: ADMIN, FOSTER, ADOPTER, VET, RESCUE_ORG")
        void shouldHaveExactlyFiveRoles() {
            assertThat(UserRole.values())
                    .hasSize(5)
                    .containsExactlyInAnyOrder(
                            UserRole.ADMIN,
                            UserRole.FOSTER,
                            UserRole.ADOPTER,
                            UserRole.VET,
                            UserRole.RESCUE_ORG
                    );
        }
    }

    @Nested
    @DisplayName("when checking role capabilities")
    class WhenCheckingRoleCapabilities {

        @Test
        @DisplayName("ADMIN role should be administrative")
        void adminRoleShouldBeAdministrative() {
            assertThat(UserRole.ADMIN.isAdministrative()).isTrue();
        }

        @Test
        @DisplayName("non-ADMIN roles should not be administrative")
        void nonAdminRolesShouldNotBeAdministrative() {
            assertThat(UserRole.FOSTER.isAdministrative()).isFalse();
            assertThat(UserRole.ADOPTER.isAdministrative()).isFalse();
            assertThat(UserRole.VET.isAdministrative()).isFalse();
            assertThat(UserRole.RESCUE_ORG.isAdministrative()).isFalse();
        }

        @Test
        @DisplayName("VET and RESCUE_ORG roles should require verification")
        void vetAndRescueOrgShouldRequireVerification() {
            assertThat(UserRole.VET.requiresVerification()).isTrue();
            assertThat(UserRole.RESCUE_ORG.requiresVerification()).isTrue();
        }

        @Test
        @DisplayName("ADMIN, FOSTER, and ADOPTER roles should not require verification")
        void adminFosterAdopterShouldNotRequireVerification() {
            assertThat(UserRole.ADMIN.requiresVerification()).isFalse();
            assertThat(UserRole.FOSTER.requiresVerification()).isFalse();
            assertThat(UserRole.ADOPTER.requiresVerification()).isFalse();
        }
    }
}
