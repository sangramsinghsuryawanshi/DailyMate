package com.dailymate.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank(message = "Prompt is required")
        @Size(max = 2000, message = "Prompt must be at most 2000 characters")
        String prompt,
        String conversationId) {

    public AssistantChatRequest(String prompt) {
        this(prompt, null);
    }
}
