package com.dailymate.auth.dto.response;

import com.dailymate.user.entity.UserRole;

public record UserResponse(String id, String email, String firstName, String lastName, UserRole role) {
}