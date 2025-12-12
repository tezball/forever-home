package com.example.foreverhome.service;

import com.example.foreverhome.config.EmailVerificationProperties;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.RefreshToken;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.auth.LoginRequest;
import com.example.foreverhome.dto.auth.LoginResponse;
import com.example.foreverhome.dto.auth.RegisterRequest;
import com.example.foreverhome.dto.auth.RegisterResponse;
import com.example.foreverhome.dto.user.UserDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserJourneyLogger journeyLogger;
    private final EmailVerificationProperties verificationProperties;
    private final MetricsService metricsService;
    private final FosterRepository fosterRepository;
    private final AdopterRepository adopterRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       UserJourneyLogger journeyLogger,
                       EmailVerificationProperties verificationProperties,
                       MetricsService metricsService,
                       FosterRepository fosterRepository,
                       AdopterRepository adopterRepository,
                       VetRepository vetRepository,
                       RescueOrganizationRepository rescueOrganizationRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.journeyLogger = journeyLogger;
        this.verificationProperties = verificationProperties;
        this.metricsService = metricsService;
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
    }

    public RegisterResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            journeyLogger.logAuth(UserJourneyLogger.ACTION_REGISTER, request.email(), false,
                "Email already exists");
            throw new EmailAlreadyExistsException(request.email());
        }

        // Create new user
        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), passwordHash, request.role(), request.name());

        // Handle email verification based on configuration
        if (verificationProperties.autoActivate()) {
            // Auto-activate user in dev mode
            user.activate();
            logger.info("Auto-activated user {} (app.email.verification.auto-activate=true)", request.email());
        } else {
            // Generate email verification token
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);

            // Send verification email
            emailService.sendVerificationEmail(request.email(), verificationToken);
        }

        // Save user
        userRepository.save(user);
        user.markNotNew();  // Mark as not new for subsequent saves

        // Auto-create role-specific profile
        createRoleProfile(user.getId(), request.role(), request.name());

        // Mark profile as complete since we auto-created it
        user.markProfileComplete();
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(request.email(), request.name());

        // Record metric
        metricsService.recordUserRegistration(request.role().name());

        journeyLogger.logAuth(UserJourneyLogger.ACTION_REGISTER, request.email(), true,
            "User registered with role " + request.role());

        return RegisterResponse.success(request.email());
    }

    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> {
                    journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), false,
                        "User not found");
                    return new AuthenticationException("Invalid credentials");
                });

        // Check if account is locked
        if (user.isLocked()) {
            journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), false,
                "Account locked");
            throw new AuthenticationException("Account is locked due to too many failed login attempts. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLoginAttempt();
            userRepository.save(user);
            metricsService.recordLoginAttempt(false);
            journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), false,
                "Invalid password (attempt " + user.getFailedLoginAttempts() + ")");
            throw new AuthenticationException("Invalid credentials");
        }

        // Check account status
        if (user.getStatus() == AccountStatus.PENDING) {
            journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), false,
                "Email not verified");
            throw new AuthenticationException("Please verify your email before logging in");
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), false,
                "Account suspended");
            throw new AuthenticationException("Your account has been suspended");
        }

        // Reset failed attempts and record login
        user.resetFailedLoginAttempts();
        user.recordLogin();
        userRepository.save(user);

        // Record successful login metric
        metricsService.recordLoginAttempt(true);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();

        // Create and save refresh token (7 days validity)
        RefreshToken refreshToken = RefreshToken.create(
                user.getId(),
                refreshTokenValue,
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, request.email(), true,
            "Login successful as " + user.getRole());

        return new LoginResponse(
                accessToken,
                refreshTokenValue,
                UserDto.from(user)
        );
    }

    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshTokenValue) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if token is revoked
        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (refreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Generate new access token
        return jwtTokenProvider.generateAccessToken(user);
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        user.activate();
        user.clearEmailVerificationToken();
        userRepository.save(user);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email.toLowerCase())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setPasswordResetToken(resetToken);
                    user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
                    userRepository.save(user);

                    emailService.sendPasswordResetEmail(email, resetToken);
                });
        // Silent failure for non-existent emails (security best practice)
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        // Check if token has expired
        if (user.getPasswordResetTokenExpiry() == null ||
            Instant.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        // Update password
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.updatePasswordHash(newPasswordHash);
        user.clearPasswordResetToken();
        userRepository.save(user);
    }

    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Auto-creates the role-specific profile during registration.
     * This allows users to immediately use role-specific features without completing a separate profile step.
     */
    private void createRoleProfile(UUID userId, UserRole role, String fullName) {
        // Parse full name into first and last name
        String[] nameParts = fullName.trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

        switch (role) {
            case FOSTER -> {
                Foster foster = Foster.create(userId, firstName, lastName, null, null);
                fosterRepository.save(foster);
                logger.info("Auto-created Foster profile for user {}", userId);
            }
            case ADOPTER -> {
                Adopter adopter = Adopter.create(userId, firstName, lastName, null, null, null, null);
                adopterRepository.save(adopter);
                logger.info("Auto-created Adopter profile for user {}", userId);
            }
            case VET -> {
                // Vets require clinic name and license number, use placeholders
                Vet vet = Vet.create(userId, "Clinic - " + fullName, "PENDING", null, null, null, null);
                vetRepository.save(vet);
                logger.info("Auto-created Vet profile for user {}", userId);
            }
            case RESCUE_ORG -> {
                // Rescue orgs require organization name
                RescueOrganization org = RescueOrganization.create(userId, "Organization - " + fullName,
                        null, null, null, firstName + " " + lastName, null, null, null);
                rescueOrganizationRepository.save(org);
                logger.info("Auto-created RescueOrganization profile for user {}", userId);
            }
            case ADMIN -> {
                // Admins don't need a profile, they use the Admin entity created elsewhere
                logger.info("Admin role - no profile auto-creation needed for user {}", userId);
            }
        }
    }
}
