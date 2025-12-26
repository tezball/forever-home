package com.example.foreverhome.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Google OAuth authentication.
 */
public record GoogleAuthRequest(
    @NotBlank(message = "ID token is required")
    String idToken
) {}
