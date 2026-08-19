package com.dailymate.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminComplaintStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(OPEN|IN_REVIEW|RESOLVED)$", message = "Status must be OPEN, IN_REVIEW, or RESOLVED")
        String status) {
}
