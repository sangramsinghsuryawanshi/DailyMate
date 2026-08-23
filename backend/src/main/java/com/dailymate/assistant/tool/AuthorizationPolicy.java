package com.dailymate.assistant.tool;

import java.util.Set;

/**
 * Encapsulates the complete authorization, visibility, sensitivity, and scope contract for an AI tool.
 */
public record AuthorizationPolicy(
        ToolScope scope,
        OperationScope operationScope,
        ResourceVisibility visibility,
        TargetScope targetScope,
        Set<String> allowedRoles,
        boolean ownershipRequired,
        DataSensitivity dataSensitivity) {
}
