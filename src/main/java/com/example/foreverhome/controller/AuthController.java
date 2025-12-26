package com.example.foreverhome.controller;

import com.example.foreverhome.dto.auth.*;
import com.example.foreverhome.dto.user.UserDto;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.AuthService;
import com.example.foreverhome.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getCurrentUser(principal.userId());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var result = authService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(Map.of(
            "accessToken", result.accessToken(),
            "refreshToken", result.refreshToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/{token}")
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of(
            "message", "If the email exists and is unverified, a new verification link has been sent"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    // ==================== Google OAuth Endpoints ====================

    /**
     * Authenticate with Google ID token.
     * For new users, returns newUser=true with email/name/googleId for role selection.
     * For existing users, returns tokens directly.
     */
    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        GoogleAuthResponse response = googleAuthService.authenticateWithGoogle(request.idToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Complete registration for a new Google user after role selection.
     */
    @PostMapping("/google/complete-registration")
    public ResponseEntity<LoginResponse> completeGoogleRegistration(
            @Valid @RequestBody GoogleCompleteRegistrationRequest request) {
        LoginResponse response = googleAuthService.completeGoogleRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Link a Google account to an existing user.
     * The Google account email must match the user's email.
     */
    @PostMapping("/google/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> linkGoogleAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GoogleLinkRequest request) {
        googleAuthService.linkGoogleAccount(principal.userId(), request.idToken());
        return ResponseEntity.ok(Map.of("message", "Google account linked successfully"));
    }

    /**
     * Unlink a Google account from an existing user.
     * The user must have a password set to unlink Google.
     */
    @DeleteMapping("/google/unlink")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> unlinkGoogleAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        googleAuthService.unlinkGoogleAccount(principal.userId());
        return ResponseEntity.ok(Map.of("message", "Google account unlinked successfully"));
    }
}
