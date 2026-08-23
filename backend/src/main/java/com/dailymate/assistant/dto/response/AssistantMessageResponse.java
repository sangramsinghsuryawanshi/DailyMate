package com.dailymate.assistant.dto.response;

import java.time.Instant;

public record AssistantMessageResponse(
        String id,
        String conversationId,
        String role,
        String content,
        AssistantActionProposalResponse proposedAction,
        Instant createdAt
) {}
