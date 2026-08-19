package com.dailymate.marketplace.dto.response;

public record ServiceProviderResponse(
        String id,
        String name,
        String category,
        String description,
        String serviceArea,
        String phone,
        String email) {
}
