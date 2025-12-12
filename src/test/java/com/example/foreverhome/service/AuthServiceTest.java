package com.example.foreverhome.service;

import com.example.foreverhome.config.EmailVerificationProperties;
import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.RefreshToken;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.auth.LoginRequest;
import com.example.foreverhome.dto.auth.LoginResponse;
import com.example.foreverhome.dto.auth.RegisterRequest;
import com.example.foreverhome.dto.auth.RegisterResponse;
import com.example.foreverhome.exception.AuthenticationException;
import com.example.foreverhome.exception.EmailAlreadyExistsException;
import com.example.foreverhome.exception.InvalidTokenException;
import com.example.foreverhome.logging.UserJourneyLogger;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.RefreshTokenRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private UserJourneyLogger journeyLogger;

    @Mock
    private MetricsService metricsService;

    @Mock
    private FosterRepository fosterRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private RescueOrganizationRepository rescueOrganizationRepository;

    private EmailVerificationProperties verificationProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Default to normal verification flow (not auto-activate)
        verificationProperties = new EmailVerificationProperties(false, false);
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                emailService,
                journeyLogger,
                verificationProperties,
                metricsService,
                fosterRepository,
                adopterRepository,
                vetRepository,
                rescueOrganizationRepository
        );
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("given valid registration data, when register, then creates user with pending status")
        void givenValidRegistrationData_whenRegister_thenCreatesUserWithPendingStatus() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "test@example.com",
                    "password123",
                    "Test User",
                    UserRole.ADOPTER
            );
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User user = inv.getArgument(0);
                return user;
            });

            // When
            RegisterResponse response = authService.register(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.message()).contains("verification");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(2)).save(userCaptor.capture());
            // First save is initial user creation, second is after profile creation
            User savedUser = userCaptor.getAllValues().get(1);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.ADOPTER);
        }

        @Test
        @DisplayName("given existing email, when register, then throws EmailAlreadyExistsException")
        void givenExistingEmail_whenRegister_thenThrowsEmailAlreadyExistsException() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "existing@example.com",
                    "password123",
                    "Test User",
                    UserRole.ADOPTER
            );
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("existing@example.com");
        }

        @Test
        @DisplayName("given valid registration, when register, then sends verification email")
        void givenValidRegistration_whenRegister_thenSendsVerificationEmail() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "test@example.com",
                    "password123",
                    "Test User",
                    UserRole.FOSTER
            );
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.register(request);

            // Then
            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("given auto-activate enabled, when register, then creates user with active status")
        void givenAutoActivateEnabled_whenRegister_thenCreatesUserWithActiveStatus() {
            // Given - create service with auto-activate enabled
            EmailVerificationProperties autoActivateProperties = new EmailVerificationProperties(true, false);
            AuthService autoActivateAuthService = new AuthService(
                    userRepository,
                    refreshTokenRepository,
                    jwtTokenProvider,
                    passwordEncoder,
                    emailService,
                    journeyLogger,
                    autoActivateProperties,
                    metricsService,
                    fosterRepository,
                    adopterRepository,
                    vetRepository,
                    rescueOrganizationRepository
            );

            RegisterRequest request = new RegisterRequest(
                    "test@example.com",
                    "password123",
                    "Test User",
                    UserRole.ADOPTER
            );
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            autoActivateAuthService.register(request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(2)).save(userCaptor.capture());
            // Second save is after profile creation with profileComplete set
            User savedUser = userCaptor.getAllValues().get(1);
            assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("given valid credentials and active user, when login, then returns tokens")
        void givenValidCredentialsAndActiveUser_whenLogin_thenReturnsTokens() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            User user = createActiveUser();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isNotNull();
            assertThat(response.user().email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("given invalid password, when login, then throws AuthenticationException")
        void givenInvalidPassword_whenLogin_thenThrowsAuthenticationException() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            User user = createActiveUser();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("given non-existent email, when login, then throws AuthenticationException")
        void givenNonExistentEmail_whenLogin_thenThrowsAuthenticationException() {
            // Given
            LoginRequest request = new LoginRequest("unknown@example.com", "password123");
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("given pending user, when login, then throws AuthenticationException")
        void givenPendingUser_whenLogin_thenThrowsAuthenticationException() {
            // Given
            LoginRequest request = new LoginRequest("pending@example.com", "password123");
            User user = User.create("pending@example.com", "hashedPassword", UserRole.ADOPTER);
            // User starts in PENDING status
            when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("verify your email");
        }

        @Test
        @DisplayName("given suspended user, when login, then throws AuthenticationException")
        void givenSuspendedUser_whenLogin_thenThrowsAuthenticationException() {
            // Given
            LoginRequest request = new LoginRequest("suspended@example.com", "password123");
            User user = createActiveUser();
            user.suspend();
            when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        @DisplayName("given failed login attempts, when login fails 5 times, then locks account")
        void givenFailedLoginAttempts_whenLoginFails5Times_thenLocksAccount() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            User user = createActiveUser();
            user.recordFailedLoginAttempt();
            user.recordFailedLoginAttempt();
            user.recordFailedLoginAttempt();
            user.recordFailedLoginAttempt();
            // 4 previous attempts, this will be the 5th
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthenticationException.class);

            verify(userRepository).save(argThat(u -> u.isLocked()));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("given valid refresh token, when refresh, then returns new access token")
        void givenValidRefreshToken_whenRefresh_thenReturnsNewAccessToken() {
            // Given
            String tokenValue = UUID.randomUUID().toString();
            User user = createActiveUser();
            com.example.foreverhome.domain.user.RefreshToken refreshToken =
                    com.example.foreverhome.domain.user.RefreshToken.create(
                            user.getId(),
                            tokenValue,
                            Instant.now().plus(7, ChronoUnit.DAYS)
                    );
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");

            // When
            String newAccessToken = authService.refreshAccessToken(tokenValue);

            // Then
            assertThat(newAccessToken).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("given expired refresh token, when refresh, then throws InvalidTokenException")
        void givenExpiredRefreshToken_whenRefresh_thenThrowsInvalidTokenException() {
            // Given
            String tokenValue = UUID.randomUUID().toString();
            User user = createActiveUser();
            com.example.foreverhome.domain.user.RefreshToken refreshToken =
                    com.example.foreverhome.domain.user.RefreshToken.create(
                            user.getId(),
                            tokenValue,
                            Instant.now().minus(1, ChronoUnit.DAYS) // Expired
                    );
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));

            // When/Then
            assertThatThrownBy(() -> authService.refreshAccessToken(tokenValue))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("given revoked refresh token, when refresh, then throws InvalidTokenException")
        void givenRevokedRefreshToken_whenRefresh_thenThrowsInvalidTokenException() {
            // Given
            String tokenValue = UUID.randomUUID().toString();
            User user = createActiveUser();
            com.example.foreverhome.domain.user.RefreshToken refreshToken =
                    com.example.foreverhome.domain.user.RefreshToken.create(
                            user.getId(),
                            tokenValue,
                            Instant.now().plus(7, ChronoUnit.DAYS)
                    );
            refreshToken.revoke();
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));

            // When/Then
            assertThatThrownBy(() -> authService.refreshAccessToken(tokenValue))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("revoked");
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("given valid verification token, when verify, then activates user")
        void givenValidVerificationToken_whenVerify_thenActivatesUser() {
            // Given
            String token = UUID.randomUUID().toString();
            User user = User.create("test@example.com", "hashedPassword", UserRole.ADOPTER);
            user.setEmailVerificationToken(token);
            when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.verifyEmail(token);

            // Then
            verify(userRepository).save(argThat(u ->
                    u.getStatus() == AccountStatus.ACTIVE &&
                    u.getEmailVerificationToken() == null
            ));
        }

        @Test
        @DisplayName("given invalid verification token, when verify, then throws InvalidTokenException")
        void givenInvalidVerificationToken_whenVerify_thenThrowsInvalidTokenException() {
            // Given
            String token = "invalid-token";
            when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.verifyEmail(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid verification token");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("given valid refresh token, when logout, then revokes token")
        void givenValidRefreshToken_whenLogout_thenRevokesToken() {
            // Given
            String tokenValue = UUID.randomUUID().toString();
            User user = createActiveUser();
            com.example.foreverhome.domain.user.RefreshToken refreshToken =
                    com.example.foreverhome.domain.user.RefreshToken.create(
                            user.getId(),
                            tokenValue,
                            Instant.now().plus(7, ChronoUnit.DAYS)
                    );
            when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.logout(tokenValue);

            // Then
            verify(refreshTokenRepository).save(argThat(t -> t.isRevoked()));
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("given existing email, when forgot password, then sends reset email")
        void givenExistingEmail_whenForgotPassword_thenSendsResetEmail() {
            // Given
            String email = "test@example.com";
            User user = createActiveUser();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.forgotPassword(email);

            // Then
            verify(emailService).sendPasswordResetEmail(eq(email), anyString());
            verify(userRepository).save(argThat(u -> u.getPasswordResetToken() != null));
        }

        @Test
        @DisplayName("given non-existent email, when forgot password, then does nothing silently")
        void givenNonExistentEmail_whenForgotPassword_thenDoesNothingSilently() {
            // Given
            String email = "unknown@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            authService.forgotPassword(email);

            // Then - Should not throw, should not send email (security - don't reveal if email exists)
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("given valid reset token, when reset password, then updates password")
        void givenValidResetToken_whenResetPassword_thenUpdatesPassword() {
            // Given
            String token = UUID.randomUUID().toString();
            String newPassword = "newPassword123";
            User user = createActiveUser();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            authService.resetPassword(token, newPassword);

            // Then
            verify(userRepository).save(argThat(u ->
                    u.getPasswordHash().equals("newHashedPassword") &&
                    u.getPasswordResetToken() == null
            ));
        }

        @Test
        @DisplayName("given expired reset token, when reset password, then throws InvalidTokenException")
        void givenExpiredResetToken_whenResetPassword_thenThrowsInvalidTokenException() {
            // Given
            String token = UUID.randomUUID().toString();
            User user = createActiveUser();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired
            when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> authService.resetPassword(token, "newPassword"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }
    }

    private User createActiveUser() {
        User user = User.create("test@example.com", "hashedPassword", UserRole.ADOPTER);
        user.activate();
        return user;
    }
}
