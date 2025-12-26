package com.example.foreverhome.dto.auth;

import com.example.foreverhome.domain.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for completing Google OAuth registration with role selection.
 */
public record GoogleCompleteRegistrationRequest(
    @NotBlank(message = "Google ID is required")
    String googleId,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Role is required")
    UserRole role
) {}
