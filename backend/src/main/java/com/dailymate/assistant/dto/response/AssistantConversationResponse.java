package com.dailymate.assistant.dto.response;

import java.time.Instant;

public record AssistantConversationResponse(
        String id,
        String userId,
        String title,
        String prompt,
        String response,
        Instant createdAt) {
}
