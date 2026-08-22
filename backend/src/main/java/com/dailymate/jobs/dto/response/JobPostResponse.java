package com.dailymate.jobs.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record JobPostResponse(
        String id,
        String userId,
        String title,
        String category,
        String location,
        String type,
        BigDecimal salary,
        String companyName,
        String contactPhone,
        String contactEmail,
        String status,
        String description,
        Instant createdAt,
        Instant updatedAt) {
}
