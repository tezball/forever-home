package com.example.foreverhome.dto.auth;

import com.example.foreverhome.dto.user.UserDto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}
