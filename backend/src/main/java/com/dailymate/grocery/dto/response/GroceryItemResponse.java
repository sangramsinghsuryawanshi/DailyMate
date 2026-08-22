package com.dailymate.grocery.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record GroceryItemResponse(
        String id,
        String userId,
        String name,
        String category,
        String store,
        BigDecimal price,
        String unit,
        String location,
        Instant createdAt,
        Instant updatedAt) {
}
