package com.example.foreverhome.controller;

import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.auth.*;
import com.example.foreverhome.dto.user.UserDto;
import com.example.foreverhome.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("given valid registration request, when register, then returns created response")
        void givenValidRegistrationRequest_whenRegister_thenReturnsCreatedResponse() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "test@example.com",
                    "password123",
                    "Test User",
                    UserRole.ADOPTER
            );
            RegisterResponse expectedResponse = new RegisterResponse(
                    "test@example.com",
                    "Please check your email to verify your account"
            );
            when(authService.register(request)).thenReturn(expectedResponse);

            // When
            ResponseEntity<RegisterResponse> response = authController.register(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(expectedResponse);
            assertThat(response.getBody().email()).isEqualTo("test@example.com");
            verify(authService).register(request);
        }

        @Test
        @DisplayName("given foster role, when register, then creates foster account")
        void givenFosterRole_whenRegister_thenCreatesFosterAccount() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "foster@example.com",
                    "password123",
                    "Foster User",
                    UserRole.FOSTER
            );
            RegisterResponse expectedResponse = new RegisterResponse(
                    "foster@example.com",
                    "Please check your email to verify your account"
            );
            when(authService.register(request)).thenReturn(expectedResponse);

            // When
            ResponseEntity<RegisterResponse> response = authController.register(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(authService).register(argThat(req -> req.role() == UserRole.FOSTER));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("given valid credentials, when login, then returns tokens and user info")
        void givenValidCredentials_whenLogin_thenReturnsTokensAndUserInfo() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            UUID userId = UUID.randomUUID();
            UserDto userDto = new UserDto(userId, "test@example.com", "Test User", UserRole.ADOPTER, AccountStatus.ACTIVE, true);
            LoginResponse expectedResponse = new LoginResponse(
                    "access-token",
                    "refresh-token",
                    userDto
            );
            when(authService.login(request)).thenReturn(expectedResponse);

            // When
            ResponseEntity<LoginResponse> response = authController.login(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().accessToken()).isEqualTo("access-token");
            assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
            assertThat(response.getBody().user().email()).isEqualTo("test@example.com");
            verify(authService).login(request);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("given valid refresh token, when refresh, then returns new access and refresh tokens")
        void givenValidRefreshToken_whenRefresh_thenReturnsNewAccessToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            var tokenResult = new AuthService.TokenRefreshResult("new-access-token", "new-refresh-token");
            when(authService.refreshAccessToken("valid-refresh-token")).thenReturn(tokenResult);

            // When
            ResponseEntity<Map<String, String>> response = authController.refresh(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("accessToken", "new-access-token");
            assertThat(response.getBody()).containsEntry("refreshToken", "new-refresh-token");
            verify(authService).refreshAccessToken("valid-refresh-token");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("given valid refresh token, when logout, then returns no content")
        void givenValidRefreshToken_whenLogout_thenReturnsNoContent() {
            // Given
            LogoutRequest request = new LogoutRequest("refresh-token");
            doNothing().when(authService).logout("refresh-token");

            // When
            ResponseEntity<Void> response = authController.logout(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout("refresh-token");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/verify-email/{token}")
    class VerifyEmail {

        @Test
        @DisplayName("given valid verification token, when verifyEmail, then returns success message")
        void givenValidVerificationToken_whenVerifyEmail_thenReturnsSuccessMessage() {
            // Given
            String token = "valid-verification-token";
            doNothing().when(authService).verifyEmail(token);

            // When
            ResponseEntity<Map<String, String>> response = authController.verifyEmail(token);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("message", "Email verified successfully");
            verify(authService).verifyEmail(token);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/resend-verification")
    class ResendVerification {

        @Test
        @DisplayName("given email, when resendVerification, then returns generic success message")
        void givenEmail_whenResendVerification_thenReturnsGenericSuccessMessage() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
            doNothing().when(authService).resendVerificationEmail("test@example.com");

            // When
            ResponseEntity<Map<String, String>> response = authController.resendVerification(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("message")).contains("If the email exists and is unverified");
            verify(authService).resendVerificationEmail("test@example.com");
        }

        @Test
        @DisplayName("given non-existent email, when resendVerification, then still returns generic message for security")
        void givenNonExistentEmail_whenResendVerification_thenStillReturnsGenericMessageForSecurity() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest("unknown@example.com");
            // Service silently handles non-existent emails
            doNothing().when(authService).resendVerificationEmail("unknown@example.com");

            // When
            ResponseEntity<Map<String, String>> response = authController.resendVerification(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Security: same response for existing and non-existing emails
            assertThat(response.getBody().get("message")).contains("If the email exists and is unverified");
        }

        @Test
        @DisplayName("given already verified email, when resendVerification, then throws exception")
        void givenAlreadyVerifiedEmail_whenResendVerification_thenThrowsException() {
            // Given
            ResendVerificationRequest request = new ResendVerificationRequest("verified@example.com");
            doThrow(new IllegalStateException("Account is already verified"))
                    .when(authService).resendVerificationEmail("verified@example.com");

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> authController.resendVerification(request)
            );
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("given email, when forgotPassword, then returns generic message for security")
        void givenEmail_whenForgotPassword_thenReturnsGenericMessageForSecurity() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
            doNothing().when(authService).forgotPassword("test@example.com");

            // When
            ResponseEntity<Map<String, String>> response = authController.forgotPassword(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("message")).contains("If the email exists");
            verify(authService).forgotPassword("test@example.com");
        }

        @Test
        @DisplayName("given non-existent email, when forgotPassword, then still returns generic message")
        void givenNonExistentEmail_whenForgotPassword_thenStillReturnsGenericMessage() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");
            doNothing().when(authService).forgotPassword("unknown@example.com");

            // When
            ResponseEntity<Map<String, String>> response = authController.forgotPassword(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Security: same response for existing and non-existing emails
            assertThat(response.getBody().get("message")).contains("If the email exists");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("given valid token and new password, when resetPassword, then returns success message")
        void givenValidTokenAndNewPassword_whenResetPassword_thenReturnsSuccessMessage() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123");
            doNothing().when(authService).resetPassword("valid-token", "newPassword123");

            // When
            ResponseEntity<Map<String, String>> response = authController.resetPassword(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("message", "Password reset successfully");
            verify(authService).resetPassword("valid-token", "newPassword123");
        }
    }
}
