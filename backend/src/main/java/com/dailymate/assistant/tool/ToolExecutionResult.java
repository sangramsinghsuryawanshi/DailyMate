package com.dailymate.assistant.tool;

import java.time.Instant;

public record ToolExecutionResult(
        boolean success,
        String toolName,
        String status,
        String resourceType,
        String resourceId,
        String resultMessage,
        Object data,
        String auditId,
        Instant executedAt) {

    public static ToolExecutionResult ok(String toolName, String resourceType, String resourceId, String message, Object data, String auditId) {
        return new ToolExecutionResult(true, toolName, "SUCCESS", resourceType, resourceId, message, data, auditId, Instant.now());
    }

    public static ToolExecutionResult failure(String toolName, String message, String auditId) {
        return new ToolExecutionResult(false, toolName, "FAILED", null, null, message, null, auditId, Instant.now());
    }
}
