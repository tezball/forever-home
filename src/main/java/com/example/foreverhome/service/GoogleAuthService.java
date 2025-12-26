package com.example.foreverhome.service;

import com.example.foreverhome.config.GoogleOAuthProperties;
import com.example.foreverhome.domain.profile.Adopter;
import com.example.foreverhome.domain.profile.Foster;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.domain.user.RefreshToken;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.dto.auth.GoogleAuthResponse;
import com.example.foreverhome.dto.auth.GoogleCompleteRegistrationRequest;
import com.example.foreverhome.dto.auth.LoginResponse;
import com.example.foreverhome.dto.user.UserDto;
import com.example.foreverhome.exception.AuthenticationException;
import com.example.foreverhome.logging.UserJourneyLogger;
import com.example.foreverhome.repository.AdopterRepository;
import com.example.foreverhome.repository.FosterRepository;
import com.example.foreverhome.repository.RefreshTokenRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

/**
 * Service for handling Google OAuth2 authentication.
 */
@Service
@Transactional
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);

    private final GoogleOAuthProperties googleProperties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserJourneyLogger journeyLogger;
    private final MetricsService metricsService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final FosterRepository fosterRepository;
    private final AdopterRepository adopterRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(GoogleOAuthProperties googleProperties,
                            UserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            JwtTokenProvider jwtTokenProvider,
                            UserJourneyLogger journeyLogger,
                            MetricsService metricsService,
                            EmailService emailService,
                            NotificationService notificationService,
                            FosterRepository fosterRepository,
                            AdopterRepository adopterRepository,
                            VetRepository vetRepository,
                            RescueOrganizationRepository rescueOrganizationRepository) {
        this.googleProperties = googleProperties;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.journeyLogger = journeyLogger;
        this.metricsService = metricsService;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;

        // Initialize Google ID token verifier
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(googleProperties.getClientId() != null
                    ? Collections.singletonList(googleProperties.getClientId())
                    : Collections.emptyList())
                .build();
    }

    /**
     * Authenticates a user with a Google ID token.
     *
     * @param idToken the Google ID token from the frontend
     * @return GoogleAuthResponse - for new users, newUser=true with email/name/googleId;
     *         for existing users, newUser=false with tokens
     * @throws AuthenticationException if the token is invalid or verification fails
     */
    public GoogleAuthResponse authenticateWithGoogle(String idToken) {
        // Verify the ID token
        GoogleIdToken.Payload payload = verifyIdToken(idToken);
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        logger.info("Google authentication for email: {}", email);

        // Check if user exists by Google ID (already linked)
        var existingGoogleUser = userRepository.findByGoogleId(googleId);
        if (existingGoogleUser.isPresent()) {
            User user = existingGoogleUser.get();
            logger.info("Existing Google user found: {}", email);
            return loginExistingUser(user);
        }

        // Check if email exists but not linked to Google
        var existingEmailUser = userRepository.findByEmail(email.toLowerCase());
        if (existingEmailUser.isPresent()) {
            User user = existingEmailUser.get();
            if (!user.isGoogleLinked()) {
                // Email exists but Google not linked - user should link from settings
                logger.info("Email {} exists but Google not linked, prompting user to link", email);
                journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, email, false,
                    "Google login - email exists but not linked");
                throw new AuthenticationException(
                    "An account with this email already exists. Please login with your password and link Google from settings.");
            }
        }

        // New user - return info for registration
        logger.info("New Google user, returning for role selection: {}", email);
        journeyLogger.logAuth("GOOGLE_AUTH", email, true, "New Google user - awaiting role selection");

        return GoogleAuthResponse.forNewUser(email, name, googleId);
    }

    /**
     * Completes registration for a new Google user after they select their role.
     *
     * @param request the registration completion request with role
     * @return LoginResponse with tokens
     */
    public LoginResponse completeGoogleRegistration(GoogleCompleteRegistrationRequest request) {
        // Validate role - admins cannot register via Google
        if (request.role() == UserRole.ADMIN) {
            throw new AuthenticationException("Admin accounts cannot be created via Google SSO");
        }

        // Check if Google ID is already registered
        if (userRepository.existsByGoogleId(request.googleId())) {
            throw new AuthenticationException("This Google account is already registered");
        }

        // Check if email is already registered
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new AuthenticationException("An account with this email already exists");
        }

        // Create user
        User user = User.createFromGoogle(
            request.email(),
            request.googleId(),
            request.role(),
            request.name()
        );
        userRepository.save(user);
        user.markNotNew();

        // Create role-specific profile
        createRoleProfile(user.getId(), request.role(), request.name());

        // Mark profile as complete
        user.markProfileComplete();
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(request.email(), request.name(), request.role());

        // Record metric
        metricsService.recordUserRegistration(request.role().name());

        // Notify admins if rescue org registered
        if (request.role() == UserRole.RESCUE_ORG) {
            notificationService.notifyAdminsPendingApproval(request.name());
        }

        journeyLogger.logAuth("GOOGLE_REGISTER", request.email(), true,
            "Google user registered with role " + request.role());

        // Generate and return tokens
        return generateTokensForUser(user);
    }

    /**
     * Links a Google account to an existing user.
     *
     * @param userId the user's ID
     * @param idToken the Google ID token
     * @throws AuthenticationException if linking fails
     */
    public void linkGoogleAccount(UUID userId, String idToken) {
        // Verify the ID token
        GoogleIdToken.Payload payload = verifyIdToken(idToken);
        String googleId = payload.getSubject();
        String googleEmail = payload.getEmail();

        // Get the user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        // Security check: Google email must match user email
        if (!user.getEmail().equalsIgnoreCase(googleEmail)) {
            logger.warn("Google link attempt with mismatched email: user={}, google={}",
                user.getEmail(), googleEmail);
            throw new AuthenticationException(
                "Google account email does not match your account email");
        }

        // Check if Google ID is already linked to another account
        if (userRepository.existsByGoogleId(googleId)) {
            throw new AuthenticationException("This Google account is already linked to another user");
        }

        // Link the account
        user.linkGoogleAccount(googleId);
        userRepository.save(user);

        journeyLogger.logAuth("GOOGLE_LINK", user.getEmail(), true, "Google account linked");
        logger.info("Google account linked for user: {}", user.getEmail());
    }

    /**
     * Unlinks a Google account from an existing user.
     *
     * @param userId the user's ID
     * @throws AuthenticationException if unlinking fails
     */
    public void unlinkGoogleAccount(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        // User must have a password to unlink Google
        if (!user.hasPassword()) {
            throw new AuthenticationException(
                "Cannot unlink Google account without a password. Please set a password first.");
        }

        user.unlinkGoogleAccount();
        userRepository.save(user);

        journeyLogger.logAuth("GOOGLE_UNLINK", user.getEmail(), true, "Google account unlinked");
        logger.info("Google account unlinked for user: {}", user.getEmail());
    }

    /**
     * Verifies a Google ID token.
     */
    private GoogleIdToken.Payload verifyIdToken(String idToken) {
        if (!googleProperties.isConfigured()) {
            throw new AuthenticationException("Google OAuth is not configured");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                logger.warn("Google ID token verification failed");
                throw new AuthenticationException("Invalid Google ID token");
            }
            return googleIdToken.getPayload();
        } catch (Exception e) {
            logger.error("Error verifying Google ID token", e);
            throw new AuthenticationException("Failed to verify Google ID token");
        }
    }

    /**
     * Logs in an existing user and returns tokens.
     */
    private GoogleAuthResponse loginExistingUser(User user) {
        // Check if user can login
        if (!user.canLogin()) {
            throw new AuthenticationException("Your account is not active");
        }

        user.recordLogin();
        userRepository.save(user);
        metricsService.recordLoginAttempt(true);

        LoginResponse loginResponse = generateTokensForUser(user);

        journeyLogger.logAuth(UserJourneyLogger.ACTION_LOGIN, user.getEmail(), true,
            "Google login successful as " + user.getRole());

        return GoogleAuthResponse.forExistingUser(
            loginResponse.accessToken(),
            loginResponse.refreshToken(),
            loginResponse.user()
        );
    }

    /**
     * Generates access and refresh tokens for a user.
     */
    private LoginResponse generateTokensForUser(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.create(
            user.getId(),
            refreshTokenValue,
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, refreshTokenValue, UserDto.from(user));
    }

    /**
     * Creates a role-specific profile for a new user.
     */
    private void createRoleProfile(UUID userId, UserRole role, String fullName) {
        String[] nameParts = fullName.trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

        switch (role) {
            case FOSTER -> {
                Foster foster = Foster.create(userId, firstName, lastName, null, null);
                fosterRepository.save(foster);
                logger.info("Auto-created Foster profile for Google user {}", userId);
            }
            case ADOPTER -> {
                Adopter adopter = Adopter.create(userId, firstName, lastName, null, null, null, null);
                adopterRepository.save(adopter);
                logger.info("Auto-created Adopter profile for Google user {}", userId);
            }
            case VET -> {
                Vet vet = Vet.create(userId, "Clinic - " + fullName, "PENDING", null, null, null, null);
                vetRepository.save(vet);
                logger.info("Auto-created Vet profile for Google user {}", userId);
            }
            case RESCUE_ORG -> {
                RescueOrganization org = RescueOrganization.create(userId, "Organization - " + fullName,
                        null, null, null, firstName + " " + lastName, null, null, null);
                rescueOrganizationRepository.save(org);
                logger.info("Auto-created RescueOrganization profile for Google user {}", userId);
            }
            case ADMIN -> {
                logger.info("Admin role - no profile auto-creation needed for Google user {}", userId);
            }
        }
    }
}
