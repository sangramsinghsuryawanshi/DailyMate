package com.dailymate.assistant.dto;

import com.dailymate.assistant.tool.BulkOperationStatus;
import java.time.Instant;
import java.util.List;

public record BulkOperationPreviewDto(
        String bulkExecutionId,
        String toolName,
        int totalRows,
        int validRows,
        int invalidRows,
        int duplicateRows,
        String previewHash,
        String confirmationPhrase,
        Instant expiresAt,
        boolean dryRun,
        String summary,
        BulkOperationStatus status,
        List<String> validationErrors) {
}
