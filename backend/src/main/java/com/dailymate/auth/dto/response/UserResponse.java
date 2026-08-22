package com.dailymate.auth.dto.response;

import com.dailymate.user.entity.UserRole;
import com.dailymate.user.entity.UserStatus;
import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        UserStatus status,
        Instant createdAt) {
}