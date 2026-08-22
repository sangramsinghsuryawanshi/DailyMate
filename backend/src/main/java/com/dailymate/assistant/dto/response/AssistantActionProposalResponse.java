package com.dailymate.assistant.dto.response;

import java.time.Instant;

public record AssistantActionProposalResponse(
        String actionId,
        String actionType,
        String summary,
        String parametersJson,
        boolean requiresConfirmation,
        String status,
        Instant expiresAt) {
}
