package com.dailymate.admin.dto.response;

import java.time.Instant;

public record AdminUserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String role,
        String status,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt) {
}
