package com.example.foreverhome.security;

import com.example.foreverhome.domain.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_MS = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, ACCESS_TOKEN_EXPIRY_MS, REFRESH_TOKEN_EXPIRY_MS);
    }

    @Nested
    @DisplayName("when generating access token")
    class WhenGeneratingAccessToken {

        @Test
        @DisplayName("should generate valid JWT token")
        void shouldGenerateValidJwtToken() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.FOSTER, true);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("should include user ID in token claims")
        void shouldIncludeUserIdInClaims() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.FOSTER, true);

            UUID extractedId = tokenProvider.getUserIdFromToken(token);
            assertThat(extractedId).isEqualTo(userId);
        }

        @Test
        @DisplayName("should include role in token claims")
        void shouldIncludeRoleInClaims() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.VET, true);

            UserRole extractedRole = tokenProvider.getRoleFromToken(token);
            assertThat(extractedRole).isEqualTo(UserRole.VET);
        }

        @Test
        @DisplayName("should include verified status in token claims")
        void shouldIncludeVerifiedStatusInClaims() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.VET, false);

            boolean verified = tokenProvider.isVerifiedFromToken(token);
            assertThat(verified).isFalse();
        }
    }

    @Nested
    @DisplayName("when generating refresh token")
    class WhenGeneratingRefreshToken {

        @Test
        @DisplayName("should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateRefreshToken(userId);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("should extract user ID from refresh token")
        void shouldExtractUserIdFromRefreshToken() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateRefreshToken(userId);

            UUID extractedId = tokenProvider.getUserIdFromToken(token);
            assertThat(extractedId).isEqualTo(userId);
        }

        @Test
        @DisplayName("should generate extended refresh token for remember me")
        void shouldGenerateExtendedRefreshTokenForRememberMe() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateRefreshToken(userId, true);

            assertThat(token).isNotBlank();
            assertThat(tokenProvider.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("when validating token")
    class WhenValidatingToken {

        @Test
        @DisplayName("should return true for valid token")
        void shouldReturnTrueForValidToken() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.FOSTER, true);

            assertThat(tokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            assertThat(tokenProvider.validateToken("not-a-valid-token")).isFalse();
        }

        @Test
        @DisplayName("should return false for token with wrong signature")
        void shouldReturnFalseForTokenWithWrongSignature() {
            UUID userId = UUID.randomUUID();
            String token = tokenProvider.generateAccessToken(userId, UserRole.FOSTER, true);
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            assertThat(tokenProvider.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            assertThat(tokenProvider.validateToken("")).isFalse();
        }
    }

    @Nested
    @DisplayName("when extracting claims from invalid token")
    class WhenExtractingClaimsFromInvalidToken {

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            assertThatThrownBy(() -> tokenProvider.getUserIdFromToken("invalid-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }
}
