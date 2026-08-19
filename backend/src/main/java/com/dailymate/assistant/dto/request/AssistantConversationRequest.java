package com.dailymate.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantConversationRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Prompt is required")
        @Size(max = 5000, message = "Prompt must be at most 5000 characters")
        String prompt,

        @NotBlank(message = "Response is required")
        @Size(max = 5000, message = "Response must be at most 5000 characters")
        String response) {
}
