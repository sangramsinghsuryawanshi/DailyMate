package com.dailymate.assistant.tool;

/**
 * Encapsulates the execution risk, preview, confirmation, idempotency, and audit requirements for an AI tool.
 */
public record OperationPolicy(
        ToolRiskTier riskTier,
        boolean confirmationRequired,
        boolean previewRequired,
        boolean idempotencyRequired,
        boolean auditRequired,
        boolean destructive,
        boolean bulkAllowed,
        boolean adminReasonRequired,
        boolean requiresConfirmationPhrase) {
}
