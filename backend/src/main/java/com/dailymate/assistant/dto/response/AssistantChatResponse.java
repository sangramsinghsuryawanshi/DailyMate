package com.dailymate.assistant.dto.response;

import java.time.Instant;

public record AssistantChatResponse(
        String id,
        String title,
        String prompt,
        String response,
        AssistantActionProposalResponse proposedAction,
        Instant createdAt) {
}
