package com.dailymate.grocery.dto.response;

import java.time.Instant;

public record GroceryItemResponse(
        String id,
        String name,
        String category,
        String store,
        double price,
        String location,
        Instant createdAt) {
}
