package com.dailymate.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminStatusUpdateRequest(
        @NotBlank(message = "Status is required")
        String status) {
}
