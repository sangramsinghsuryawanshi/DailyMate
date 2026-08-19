package com.dailymate.jobs.dto.response;

import java.time.Instant;

public record JobPostResponse(
        String id,
        String title,
        String category,
        String location,
        String type,
        String description,
        Instant createdAt) {
}
