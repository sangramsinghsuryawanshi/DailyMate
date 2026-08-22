package com.dailymate.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceProviderResponse(
        String id,
        String userId,
        String name,
        String category,
        String description,
        String serviceArea,
        String phone,
        String email,
        BigDecimal hourlyRate,
        Instant createdAt) {
}
