package com.dailymate.assistant.dto.request;

import com.dailymate.assistant.tool.TargetSelectionMode;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record CanonicalBulkRequest(
        @NotBlank(message = "Tool name is required")
        String toolName,
        TargetSelectionMode selectionMode,
        Map<String, Object> filterCriteria,
        List<Map<String, Object>> payloadRows,
        String adminReason,
        boolean dryRun) {

    public CanonicalBulkRequest {
        if (selectionMode == null) {
            selectionMode = TargetSelectionMode.BY_IMPORT;
        }
    }
}
