package com.dailymate.assistant.dto.request;

public record AssistantActionExecutionRequest(
        String idempotencyKey) {
}
