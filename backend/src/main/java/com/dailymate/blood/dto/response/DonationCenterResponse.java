package com.dailymate.blood.dto.response;

import java.time.Instant;

public record DonationCenterResponse(
        String id,
        String name,
        String location,
        String contact,
        String description,
        Instant createdAt) {
}
