package com.dailymate.assistant.dto.response;

import java.time.Instant;

public record AssistantActionExecutionResponse(
        String actionId,
        String actionType,
        String status,
        String resultMessage,
        Instant executedAt) {
}
