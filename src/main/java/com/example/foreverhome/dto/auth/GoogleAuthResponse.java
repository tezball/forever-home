package com.example.foreverhome.dto.auth;

import com.example.foreverhome.dto.user.UserDto;

/**
 * Response DTO for Google OAuth authentication.
 * For new users, newUser is true and tokens are null.
 * For existing users, newUser is false and tokens are provided.
 */
public record GoogleAuthResponse(
    boolean newUser,
    String email,
    String name,
    String googleId,
    String accessToken,
    String refreshToken,
    UserDto user
) {
    /**
     * Creates a response for a new user who needs to complete registration.
     */
    public static GoogleAuthResponse forNewUser(String email, String name, String googleId) {
        return new GoogleAuthResponse(true, email, name, googleId, null, null, null);
    }

    /**
     * Creates a response for an existing user with tokens.
     */
    public static GoogleAuthResponse forExistingUser(String accessToken, String refreshToken, UserDto user) {
        return new GoogleAuthResponse(false, null, null, null, accessToken, refreshToken, user);
    }
}
