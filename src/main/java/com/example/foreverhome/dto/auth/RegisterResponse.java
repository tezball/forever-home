package com.example.foreverhome.dto.auth;

public record RegisterResponse(
        String email,
        String message
) {
    public static RegisterResponse success(String email) {
        return new RegisterResponse(
                email,
                "Registration successful. Please check your email for a verification link."
        );
    }
}
