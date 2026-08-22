package com.dailymate.assistant.tool;

import java.util.List;
import java.util.Set;

/**
 * Permanent Universal AI Tool Contract.
 * Every registered tool must define its domain, operation type, risk tier, scope, parameters,
 * confirmation mandate, idempotency, auditability, ownership, and destructive properties.
 */
public record AssistantToolDefinition(
        String name,
        String description,
        ToolDomain domain,
        ToolOperationType operationType,
        ToolRiskTier riskTier,
        ToolScope scope,
        OperationScope operationScope,
        Set<String> allowedRoles,
        List<String> requiredParameters,
        List<String> optionalParameters,
        boolean confirmationRequired,
        boolean idempotencyRequired,
        boolean auditRequired,
        boolean ownershipRequired,
        boolean destructive,
        boolean previewRequired,
        boolean bulkAllowed) {
}
