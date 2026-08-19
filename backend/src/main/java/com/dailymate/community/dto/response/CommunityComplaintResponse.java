package com.dailymate.community.dto.response;

import java.time.Instant;

public record CommunityComplaintResponse(
        String id,
        String title,
        String category,
        String location,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
