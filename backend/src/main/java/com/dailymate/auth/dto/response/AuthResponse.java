package com.dailymate.auth.dto.response;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
		long refreshExpiresIn, UserResponse user) {
}
