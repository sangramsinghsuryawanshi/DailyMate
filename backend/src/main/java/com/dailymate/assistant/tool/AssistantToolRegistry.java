package com.dailymate.assistant.tool;

import com.dailymate.core.exception.ForbiddenException;
import com.dailymate.core.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Central Server-Authoritative AI Tool Registry.
 * Enforces permissions, risk tiers, scopes, confirmation rules, and parameter contracts.
 * Invariant: Validates all tool definitions at application startup (fail-fast).
 */
@Component
public class AssistantToolRegistry {

    private final Map<String, AssistantToolDefinition> tools = new LinkedHashMap<>();

    public AssistantToolRegistry() {
        registerBuiltInTools();
    }

    private void registerBuiltInTools() {
        // --- 1. Expense Tools ---
        register(new AssistantToolDefinition(
                "expense.record",
                "Record a new expense entry with verified amount and category",
                ToolDomain.EXPENSE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("amount", "description", "category"),
                List.of("spentOn", "notes"),
                true, // confirmation required
                true, // idempotency required
                true, // audit required
                true, // ownership required
                false, // not destructive
                false, // preview required
                false  // bulk allowed
        ));

        register(new AssistantToolDefinition(
                "expense.delete",
                "Delete an existing expense entry by ID",
                ToolDomain.EXPENSE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("expenseId"),
                List.of(),
                true,
                true,
                true,
                true,
                true, // destructive
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "expense.bulkRecord",
                "Bulk import or record multiple expense entries with batch preview",
                ToolDomain.EXPENSE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.BULK,
                Set.of("USER", "ADMIN"),
                List.of("entries"),
                List.of(),
                true,
                true,
                true,
                true,
                false,
                true, // preview required
                true  // bulk allowed
        ));

        register(new AssistantToolDefinition(
                "expense.bulkDelete",
                "Bulk delete multiple expense entries owned by user with verified preview",
                ToolDomain.EXPENSE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.BULK,
                Set.of("USER", "ADMIN"),
                List.of("expenseIds"),
                List.of(),
                true,
                true,
                true,
                true,
                true, // destructive
                true, // preview required
                true
        ));

        register(new AssistantToolDefinition(
                "expense.getSummary",
                "Get spending summary and category breakdown for the current month",
                ToolDomain.EXPENSE,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("month", "year"),
                false,
                false,
                false,
                true,
                false,
                false,
                false
        ));

        // --- 2. Medicine Reminder Tools ---
        register(new AssistantToolDefinition(
                "medicine.create",
                "Create a scheduled medicine reminder",
                ToolDomain.MEDICINE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("name", "dosage", "remindAt"),
                List.of("frequency", "notes"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "medicine.delete",
                "Delete a scheduled medicine reminder by ID",
                ToolDomain.MEDICINE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("reminderId"),
                List.of(),
                true,
                true,
                true,
                true,
                true,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "medicine.bulkCreate",
                "Bulk import or create multiple medicine reminders with preview validation",
                ToolDomain.MEDICINE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.BULK,
                Set.of("USER", "ADMIN"),
                List.of("reminders"),
                List.of(),
                true,
                true,
                true,
                true,
                false,
                true,
                true
        ));

        register(new AssistantToolDefinition(
                "medicine.list",
                "List all active medicine reminders",
                ToolDomain.MEDICINE,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of(),
                false,
                false,
                false,
                true,
                false,
                false,
                false
        ));

        // --- 3. Blood Donation Tools ---
        register(new AssistantToolDefinition(
                "blood.createRequest",
                "Create an emergency blood request with verified blood group and hospital location",
                ToolDomain.BLOOD,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("patientName", "bloodGroup", "unitsNeeded", "hospitalLocation", "urgency", "contactName", "contactPhone"),
                List.of("additionalNotes"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "blood.deleteRequest",
                "Cancel/delete an existing blood request by ID",
                ToolDomain.BLOOD,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("requestId"),
                List.of(),
                true,
                true,
                true,
                true,
                true,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "blood.searchRequests",
                "Search open community blood requests",
                ToolDomain.BLOOD,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("bloodGroup", "city"),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        // --- 4. Local Services / Marketplace Tools ---
        register(new AssistantToolDefinition(
                "marketplace.registerProvider",
                "Register a new local service provider (electrician, plumber, etc.)",
                ToolDomain.MARKETPLACE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("name", "serviceType", "phone", "city"),
                List.of("area", "experienceYears"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "marketplace.adminBulkActivate",
                "Admin bulk status update or activation for local service providers",
                ToolDomain.MARKETPLACE,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.ADMIN,
                OperationScope.BULK,
                Set.of("ADMIN"), // Strictly Admin
                List.of("providerIds", "targetStatus"),
                List.of(),
                true,
                true,
                true,
                false,
                false,
                true,
                true
        ));

        register(new AssistantToolDefinition(
                "marketplace.search",
                "Search verified local service providers by category and location",
                ToolDomain.MARKETPLACE,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("serviceType", "city"),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        // --- 5. Notification Tools ---
        register(new AssistantToolDefinition(
                "notification.markAllRead",
                "Mark all unread notifications as read",
                ToolDomain.NOTIFICATION,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_2,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of(),
                false,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "notification.adminBulkBroadcast",
                "Admin bulk notification broadcast to targeted users or community",
                ToolDomain.NOTIFICATION,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.ADMIN,
                OperationScope.BULK,
                Set.of("ADMIN"),
                List.of("title", "message", "recipientUserIds"),
                List.of("type"),
                true,
                true,
                true,
                false,
                false,
                true,
                true
        ));

        register(new AssistantToolDefinition(
                "notification.create",
                "Create a new scheduled or instant notification alert",
                ToolDomain.NOTIFICATION,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("title", "message"),
                List.of("type", "link"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "notification.list",
                "List recent user notifications",
                ToolDomain.NOTIFICATION,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("unreadOnly"),
                false,
                false,
                false,
                true,
                false,
                false,
                false
        ));

        // --- 6. Emergency Directory Tools ---
        register(new AssistantToolDefinition(
                "emergency.createContact",
                "Add a personal In Case of Emergency (ICE) contact",
                ToolDomain.EMERGENCY,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("name", "relationship", "phone"),
                List.of("category", "notes"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "emergency.deleteContact",
                "Delete a personal ICE emergency contact by ID",
                ToolDomain.EMERGENCY,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("contactId"),
                List.of(),
                true,
                true,
                true,
                true,
                true,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "emergency.search",
                "Search national and local emergency hotlines and personal ICE contacts",
                ToolDomain.EMERGENCY,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("category"),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        // --- 7. Community Events Tools ---
        register(new AssistantToolDefinition(
                "events.create",
                "Post a new verified community event or gathering",
                ToolDomain.EVENTS,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("title", "description", "location", "eventDate", "category"),
                List.of(),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "events.search",
                "Search upcoming community events and gatherings",
                ToolDomain.EVENTS,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("category", "city"),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        // --- 8. Community Jobs Board Tools ---
        register(new AssistantToolDefinition(
                "jobs.create",
                "Post an open community job opportunity",
                ToolDomain.JOBS,
                ToolOperationType.MUTATION,
                ToolRiskTier.TIER_3,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of("title", "companyName", "location", "type", "description"),
                List.of("salary", "contactEmail"),
                true,
                true,
                true,
                true,
                false,
                false,
                false
        ));

        register(new AssistantToolDefinition(
                "jobs.search",
                "Search open community job postings and hiring opportunities",
                ToolDomain.JOBS,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of("location", "type"),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        // --- 9. Reporting & Analytics Tools ---
        register(new AssistantToolDefinition(
                "report.monthlyLifeReport",
                "Generate comprehensive deterministic monthly life report from actual DB metrics",
                ToolDomain.REPORTS,
                ToolOperationType.READ,
                ToolRiskTier.TIER_1,
                ToolScope.USER,
                OperationScope.SINGLE,
                Set.of("USER", "ADMIN"),
                List.of(),
                List.of(),
                false,
                false,
                false,
                true,
                false,
                false,
                false
        ));
    }

    public synchronized void register(AssistantToolDefinition tool) {
        validateSingleTool(tool);
        tools.put(tool.name(), tool);
    }

    @PostConstruct
    public void validateAllTools() {
        if (tools.isEmpty()) {
            throw new IllegalStateException("Tool registry must not be empty at startup.");
        }
        for (AssistantToolDefinition tool : tools.values()) {
            validateSingleTool(tool);
        }
    }

    private void validateSingleTool(AssistantToolDefinition tool) {
        if (tool.name() == null || tool.name().isBlank()) {
            throw new IllegalStateException("Tool name must not be blank.");
        }
        if (tool.domain() == null) {
            throw new IllegalStateException("Tool domain must be defined for " + tool.name());
        }
        if (tool.operationType() == null) {
            throw new IllegalStateException("Tool operationType must be defined for " + tool.name());
        }
        if (tool.riskTier() == null) {
            throw new IllegalStateException("Tool riskTier must be defined for " + tool.name());
        }
        if (tool.scope() == null) {
            throw new IllegalStateException("Tool scope must be defined for " + tool.name());
        }
        if (tool.operationScope() == null) {
            throw new IllegalStateException("Tool operationScope must be defined for " + tool.name());
        }
        if (tool.allowedRoles() == null || tool.allowedRoles().isEmpty()) {
            throw new IllegalStateException("Tool allowedRoles must not be empty for " + tool.name());
        }

        // Fail-Fast: Tier 1 READ tools must not be configured for mutation
        if (tool.operationType() == ToolOperationType.READ && tool.riskTier() != ToolRiskTier.TIER_1) {
            throw new IllegalStateException("READ operation must have TIER_1 risk classification: " + tool.name());
        }

        // Fail-Fast: Tier 3 MUTATION tools must require confirmation, idempotency, and audit
        if (tool.riskTier() == ToolRiskTier.TIER_3) {
            if (!tool.confirmationRequired()) {
                throw new IllegalStateException("TIER_3 tool MUST require confirmation: " + tool.name());
            }
            if (!tool.idempotencyRequired()) {
                throw new IllegalStateException("TIER_3 tool MUST require idempotency: " + tool.name());
            }
            if (!tool.auditRequired()) {
                throw new IllegalStateException("TIER_3 tool MUST require audit logging: " + tool.name());
            }
        }

        // Fail-Fast: Destructive operations must be Tier 3, confirmationRequired, idempotencyRequired, auditRequired
        if (tool.destructive()) {
            if (tool.riskTier() != ToolRiskTier.TIER_3 || !tool.confirmationRequired() || !tool.idempotencyRequired() || !tool.auditRequired()) {
                throw new IllegalStateException("DESTRUCTIVE tool MUST be TIER_3 with confirmation, idempotency, and audit: " + tool.name());
            }
        }

        // Fail-Fast: Bulk tools must have bulkAllowed=true and previewRequired=true if mutation
        if (tool.operationScope() == OperationScope.BULK && tool.operationType() == ToolOperationType.MUTATION) {
            if (!tool.bulkAllowed()) {
                throw new IllegalStateException("Bulk tool MUST have bulkAllowed=true: " + tool.name());
            }
            if (!tool.previewRequired()) {
                throw new IllegalStateException("Bulk mutation tool MUST have previewRequired=true: " + tool.name());
            }
        }
    }

    public AssistantToolDefinition getTool(String name) {
        AssistantToolDefinition tool = tools.get(name);
        if (tool == null) {
            throw new NotFoundException("Tool not registered: " + name);
        }
        return tool;
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public Map<String, AssistantToolDefinition> getAllTools() {
        return Collections.unmodifiableMap(tools);
    }

    public void validateAuthorization(String toolName, String userRole) {
        AssistantToolDefinition tool = getTool(toolName);
        if (userRole == null || !tool.allowedRoles().contains(userRole.toUpperCase())) {
            throw new ForbiddenException("User role " + userRole + " is not authorized to invoke tool: " + toolName);
        }
    }
}
