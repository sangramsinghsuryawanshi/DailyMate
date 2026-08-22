package com.dailymate.assistant.dto;

import com.dailymate.assistant.tool.BulkOperationStatus;
import java.time.Instant;
import java.util.List;

public record BulkExecutionResultDto(
        String bulkExecutionId,
        BulkOperationStatus status,
        int totalRows,
        int succeededRows,
        int failedRows,
        String resultMessage,
        List<String> failedRowDetails,
        Instant completedAt) {
}
