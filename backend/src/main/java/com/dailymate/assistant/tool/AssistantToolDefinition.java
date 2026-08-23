package com.dailymate.assistant.tool;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permanent Universal Server-Authoritative AI Tool Contract.
 * Every registered tool declares its identity, domain, operation type,
 * AuthorizationPolicy, OperationPolicy, parameter types, enums, array bounds,
 * side-effect classification, examples, and validation rules.
 */
public record AssistantToolDefinition(
        String name,
        String description,
        ToolDomain domain,
        ToolOperationType operationType,
        AuthorizationPolicy authPolicy,
        OperationPolicy opPolicy,
        List<String> requiredParameters,
        List<String> optionalParameters,
        Map<String, String> fieldTypes,
        Map<String, List<String>> enums,
        int minArraySize,
        int maxArraySize,
        String sideEffectClassification,
        List<String> examples,
        List<String> validationRules) {

    // Backward-compatible constructor
    public AssistantToolDefinition(
            String name,
            String description,
            ToolDomain domain,
            ToolOperationType operationType,
            AuthorizationPolicy authPolicy,
            OperationPolicy opPolicy,
            List<String> requiredParameters,
            List<String> optionalParameters) {
        this(
                name,
                description,
                domain,
                operationType,
                authPolicy,
                opPolicy,
                requiredParameters,
                optionalParameters,
                Map.of(),
                Map.of(),
                1,
                operationType == ToolOperationType.IMPORT || opPolicy.bulkAllowed() ? 500 : 1,
                operationType == ToolOperationType.READ ? "READ_ONLY" : (opPolicy.destructive() ? "DESTRUCTIVE_MUTATION" : "MUTATION"),
                List.of(),
                List.of()
        );
    }

    // Convenience accessors
    public ToolRiskTier riskTier() { return opPolicy.riskTier(); }
    public ToolScope scope() { return authPolicy.scope(); }
    public OperationScope operationScope() { return authPolicy.operationScope(); }
    public Set<String> allowedRoles() { return authPolicy.allowedRoles(); }
    public boolean confirmationRequired() { return opPolicy.confirmationRequired(); }
    public boolean idempotencyRequired() { return opPolicy.idempotencyRequired(); }
    public boolean auditRequired() { return opPolicy.auditRequired(); }
    public boolean ownershipRequired() { return authPolicy.ownershipRequired(); }
    public boolean destructive() { return opPolicy.destructive(); }
    public boolean previewRequired() { return opPolicy.previewRequired(); }
    public boolean bulkAllowed() { return opPolicy.bulkAllowed(); }
    public boolean adminReasonRequired() { return opPolicy.adminReasonRequired(); }
    public boolean requiresConfirmationPhrase() { return opPolicy.requiresConfirmationPhrase(); }
    public DataSensitivity dataSensitivity() { return authPolicy.dataSensitivity(); }
    public ResourceVisibility visibility() { return authPolicy.visibility(); }
    public TargetScope targetScope() { return authPolicy.targetScope(); }
}
