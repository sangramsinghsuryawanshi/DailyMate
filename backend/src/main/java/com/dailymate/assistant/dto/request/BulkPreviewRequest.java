package com.dailymate.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record BulkPreviewRequest(
        @NotBlank(message = "Tool name is required")
        String toolName,
        @NotEmpty(message = "Payload rows must not be empty")
        List<Map<String, Object>> payloadRows) {
}
