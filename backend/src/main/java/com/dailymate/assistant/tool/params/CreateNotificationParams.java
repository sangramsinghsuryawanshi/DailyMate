package com.dailymate.assistant.tool.params;

public record CreateNotificationParams(
        String title,
        String message,
        String type,
        String link) {
}
