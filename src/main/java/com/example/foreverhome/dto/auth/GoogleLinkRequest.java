package com.example.foreverhome.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking a Google account to an existing user.
 */
public record GoogleLinkRequest(
    @NotBlank(message = "ID token is required")
    String idToken
) {}
