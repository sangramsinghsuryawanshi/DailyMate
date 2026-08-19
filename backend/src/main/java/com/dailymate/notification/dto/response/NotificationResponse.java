package com.dailymate.notification.dto.response;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String title,
        String message,
        String type,
        boolean read,
        Instant createdAt,
        Instant updatedAt,
        String targetType,
        String targetId,
        String targetUrl) {
}
