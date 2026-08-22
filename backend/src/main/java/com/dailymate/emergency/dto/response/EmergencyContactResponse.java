package com.dailymate.emergency.dto.response;

import java.time.Instant;

public record EmergencyContactResponse(
        String id,
        String userId,
        String name,
        String category,
        String phone,
        String location,
        String description,
        Instant createdAt) {
}
