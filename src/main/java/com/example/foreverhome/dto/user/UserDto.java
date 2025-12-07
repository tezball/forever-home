package com.example.foreverhome.dto.user;

import com.example.foreverhome.domain.user.AccountStatus;
import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        UserRole role,
        AccountStatus status,
        boolean profileComplete
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.isProfileComplete()
        );
    }
}
