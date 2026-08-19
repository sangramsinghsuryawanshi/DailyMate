package com.dailymate.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        @NotBlank(message = "Type is required")
        @Size(max = 40, message = "Type must be at most 40 characters")
        String type,

        boolean read,

        @Size(max = 64)
        String targetType,

        @Size(max = 64)
        String targetId,

        @Size(max = 1024)
        String targetUrl) {
}
