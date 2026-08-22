package com.dailymate.events.dto.response;

import java.time.Instant;

public record LocalEventResponse(
        String id,
        String userId,
        String title,
        String category,
        String location,
        Instant eventDate,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
