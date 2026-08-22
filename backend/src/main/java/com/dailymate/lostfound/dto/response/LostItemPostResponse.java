package com.dailymate.lostfound.dto.response;

import java.time.Instant;

public record LostItemPostResponse(
        String id,
        String userId,
        String title,
        String itemType,
        String location,
        String description,
        String contactName,
        String contactPhone,
        Instant createdAt,
        Instant updatedAt) {
}
