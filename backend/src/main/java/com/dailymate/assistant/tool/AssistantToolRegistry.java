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
 * Enforces permissions, risk tiers, policies, confirmation rules, and parameter contracts.
 * Invariant: Validates all tool definitions at application startup (fail-fast).
 */
@Component
public class AssistantToolRegistry {

    private final Map<String, AssistantToolDefinition> tools = new LinkedHashMap<>();

    public AssistantToolRegistry() {
        registerBuiltInTools();
    }

    private void registerBuiltInTools() {
        // --- 1. Expense Tools (User-Private & Restricted) ---
        register(new AssistantToolDefinition(
                "expense.record",
                "Record a new expense entry with verified amount and category",
                ToolDomain.EXPENSE,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("amount", "description", "category"),
                List.of("spentOn", "notes")
        ));

        register(new AssistantToolDefinition(
                "expense.update",
                "Update an existing expense entry owned by user",
                ToolDomain.EXPENSE,
                ToolOperationType.UPDATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("expenseId"),
                List.of("amount", "description", "category", "spentOn", "notes")
        ));

        register(new AssistantToolDefinition(
                "expense.delete",
                "Delete an existing expense entry by ID",
                ToolDomain.EXPENSE,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, true, false, false, false),
                List.of("expenseId"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "expense.bulkRecord",
                "Bulk import or record multiple expense entries with batch preview",
                ToolDomain.EXPENSE,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("entries"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "expense.bulkDelete",
                "Bulk delete multiple expense entries owned by user with verified preview",
                ToolDomain.EXPENSE,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, false, true),
                List.of("expenseIds"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "expense.getSummary",
                "Get spending summary and category breakdown for the current month",
                ToolDomain.EXPENSE,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("month", "year")
        ));

        // --- 2. Medicine Reminder Tools (User-Private & Confidential) ---
        register(new AssistantToolDefinition(
                "medicine.create",
                "Create a scheduled medicine reminder",
                ToolDomain.MEDICINE,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("name", "dosage", "remindAt"),
                List.of("frequency", "notes")
        ));

        register(new AssistantToolDefinition(
                "medicine.update",
                "Update an existing scheduled medicine reminder",
                ToolDomain.MEDICINE,
                ToolOperationType.UPDATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("reminderId"),
                List.of("name", "dosage", "remindAt", "frequency", "notes")
        ));

        register(new AssistantToolDefinition(
                "medicine.delete",
                "Delete a scheduled medicine reminder by ID",
                ToolDomain.MEDICINE,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, true, false, false, false),
                List.of("reminderId"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "medicine.bulkCreate",
                "Bulk import or create multiple medicine reminders with preview validation",
                ToolDomain.MEDICINE,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("reminders"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "medicine.bulkDelete",
                "Bulk delete multiple scheduled medicine reminders",
                ToolDomain.MEDICINE,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, false, true),
                List.of("reminderIds"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "medicine.list",
                "List all active medicine reminders for authenticated user",
                ToolDomain.MEDICINE,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of()
        ));

        // --- 3. Blood Donation Tools (User & Public) ---
        register(new AssistantToolDefinition(
                "blood.createRequest",
                "Create an emergency blood request with verified blood group and hospital location",
                ToolDomain.BLOOD,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("patientName", "bloodGroup", "unitsNeeded", "hospitalLocation", "urgency", "contactName", "contactPhone"),
                List.of("additionalNotes")
        ));

        register(new AssistantToolDefinition(
                "blood.deleteRequest",
                "Cancel/delete an existing blood request owned by user",
                ToolDomain.BLOOD,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, true, false, false, false),
                List.of("requestId"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "blood.searchRequests",
                "Search open community blood requests",
                ToolDomain.BLOOD,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("bloodGroup", "city")
        ));

        register(new AssistantToolDefinition(
                "blood.bulkCreateRequests",
                "Bulk import or post multiple emergency blood donation requests",
                ToolDomain.BLOOD,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("requests"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "blood.admin.bulkArchive",
                "Admin bulk archive or close fulfilled/expired community blood requests",
                ToolDomain.BLOOD,
                ToolOperationType.ARCHIVE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("requestIds"),
                List.of()
        ));

        // --- 4. Local Services / Marketplace Tools ---
        register(new AssistantToolDefinition(
                "marketplace.registerProvider",
                "Register a new local service provider (electrician, plumber, etc.)",
                ToolDomain.MARKETPLACE,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("name", "serviceType", "phone", "city"),
                List.of("area", "experienceYears")
        ));

        register(new AssistantToolDefinition(
                "marketplace.bulkRegister",
                "Bulk register multiple local service providers",
                ToolDomain.MARKETPLACE,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("providers"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "marketplace.search",
                "Search verified local service providers by category and location",
                ToolDomain.MARKETPLACE,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("serviceType", "city")
        ));

        register(new AssistantToolDefinition(
                "marketplace.admin.bulkVerify",
                "Admin bulk verify local service providers by IDs or query filter",
                ToolDomain.MARKETPLACE,
                ToolOperationType.VERIFY,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.FILTERED_USERS, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, true, false),
                List.of("providerIds"),
                List.of("filterCriteria")
        ));

        register(new AssistantToolDefinition(
                "marketplace.admin.bulkDisable",
                "Admin bulk deactivate/disable unverified or violating service providers",
                ToolDomain.MARKETPLACE,
                ToolOperationType.DEACTIVATE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.FILTERED_USERS, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("providerIds"),
                List.of("filterCriteria")
        ));

        // --- 5. Notification Tools ---
        register(new AssistantToolDefinition(
                "notification.markAllRead",
                "Mark all unread notifications as read for current user",
                ToolDomain.NOTIFICATION,
                ToolOperationType.UPDATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_2, false, false, true, true, false, false, false, false),
                List.of(),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "notification.create",
                "Create a personal scheduled or instant notification alert for self",
                ToolDomain.NOTIFICATION,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("title", "message"),
                List.of("type", "link")
        ));

        register(new AssistantToolDefinition(
                "notification.admin.bulkBroadcast",
                "Admin bulk notification broadcast to targeted users or entire community",
                ToolDomain.NOTIFICATION,
                ToolOperationType.BROADCAST,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.FILTERED_USERS, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, true, false),
                List.of("title", "message", "recipientUserIds"),
                List.of("type")
        ));

        register(new AssistantToolDefinition(
                "notification.list",
                "List recent user notifications",
                ToolDomain.NOTIFICATION,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("unreadOnly")
        ));

        register(new AssistantToolDefinition(
                "notification.bulkCreate",
                "Bulk create personal notifications or alerts",
                ToolDomain.NOTIFICATION,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("notifications"),
                List.of()
        ));

        // --- 6. Emergency Directory Tools ---
        register(new AssistantToolDefinition(
                "emergency.createContact",
                "Add a personal In Case of Emergency (ICE) contact",
                ToolDomain.EMERGENCY,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("name", "relationship", "phone"),
                List.of("category", "notes")
        ));

        register(new AssistantToolDefinition(
                "emergency.bulkCreate",
                "Bulk import or create multiple personal ICE emergency contacts",
                ToolDomain.EMERGENCY,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("contacts"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "emergency.deleteContact",
                "Delete a personal ICE emergency contact by ID",
                ToolDomain.EMERGENCY,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.CONFIDENTIAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, true, false, false, false),
                List.of("contactId"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "emergency.search",
                "Search national and local emergency hotlines and personal ICE contacts",
                ToolDomain.EMERGENCY,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("category")
        ));

        // --- 7. Community Events Tools ---
        register(new AssistantToolDefinition(
                "events.create",
                "Post a new verified community event or gathering",
                ToolDomain.EVENTS,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("title", "description", "location", "eventDate", "category"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "events.bulkCreate",
                "Bulk post multiple verified community events",
                ToolDomain.EVENTS,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("events"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "events.search",
                "Search upcoming community events and gatherings",
                ToolDomain.EVENTS,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("category", "city")
        ));

        register(new AssistantToolDefinition(
                "events.admin.bulkArchive",
                "Admin bulk archive expired community events",
                ToolDomain.EVENTS,
                ToolOperationType.ARCHIVE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("eventIds"),
                List.of()
        ));

        // --- 8. Community Jobs Board Tools ---
        register(new AssistantToolDefinition(
                "jobs.create",
                "Post an open community job opportunity",
                ToolDomain.JOBS,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("title", "companyName", "location", "type", "description"),
                List.of("salary", "contactEmail")
        ));

        register(new AssistantToolDefinition(
                "jobs.bulkCreate",
                "Bulk post multiple open community job opportunities",
                ToolDomain.JOBS,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("jobs"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "jobs.search",
                "Search open community job postings and hiring opportunities",
                ToolDomain.JOBS,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("location", "type")
        ));

        register(new AssistantToolDefinition(
                "jobs.admin.bulkArchive",
                "Admin bulk archive expired community job posts",
                ToolDomain.JOBS,
                ToolOperationType.ARCHIVE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("jobIds"),
                List.of()
        ));

        // --- 9. Lost & Found Tools ---
        register(new AssistantToolDefinition(
                "lostFound.createItem",
                "Report a lost or found item in the community",
                ToolDomain.LOST_FOUND,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("title", "description", "location", "type"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "lostFound.bulkCreate",
                "Bulk report multiple lost or found items",
                ToolDomain.LOST_FOUND,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("items"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "lostFound.listRecent",
                "List recent lost and found posts",
                ToolDomain.LOST_FOUND,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("type", "search")
        ));

        // --- 10. Community Complaints Tools ---
        register(new AssistantToolDefinition(
                "complaint.submit",
                "Submit a civic or community infrastructure complaint",
                ToolDomain.COMPLAINT,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("title", "description", "category", "location"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "complaint.bulkSubmit",
                "Bulk submit multiple civic complaints",
                ToolDomain.COMPLAINT,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("complaints"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "complaint.listRecent",
                "List recent community complaints",
                ToolDomain.COMPLAINT,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("category", "status")
        ));

        // --- 11. Grocery Price Comparison Tools ---
        register(new AssistantToolDefinition(
                "grocery.addItem",
                "Add a grocery item price comparison record",
                ToolDomain.GROCERY,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, false, true, true, false, false, false, false),
                List.of("name", "category", "store", "price"),
                List.of("unit", "location")
        ));

        register(new AssistantToolDefinition(
                "grocery.bulkAdd",
                "Bulk add multiple grocery item price records",
                ToolDomain.GROCERY,
                ToolOperationType.IMPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.BULK, ResourceVisibility.PUBLIC, TargetScope.OWNED_RESOURCES, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, false, true, false, false),
                List.of("items"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "grocery.comparePrices",
                "Search and compare local grocery prices across stores",
                ToolDomain.GROCERY,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER", "ADMIN"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("search", "category", "store")
        ));

        // --- 9. Reporting & Analytics Tools ---
        register(new AssistantToolDefinition(
                "report.monthlyLifeReport",
                "Generate comprehensive deterministic monthly life report from actual DB metrics",
                ToolDomain.REPORTS,
                ToolOperationType.REPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "analytics.monthlyReport",
                "Get deterministic structured monthly analytics with category breakdowns and MoM comparisons",
                ToolDomain.REPORTS,
                ToolOperationType.ANALYZE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("period")
        ));

        register(new AssistantToolDefinition(
                "analytics.comparePeriods",
                "Compare deterministic financial expenditures between two monthly periods with exact MoM delta",
                ToolDomain.REPORTS,
                ToolOperationType.ANALYZE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of("period1", "period2"),
                List.of()
        ));

        register(new AssistantToolDefinition(
                "analytics.categoryBreakdown",
                "Get authoritative raw category breakdown and distribution for a specific period",
                ToolDomain.REPORTS,
                ToolOperationType.ANALYZE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("period")
        ));

        register(new AssistantToolDefinition(
                "analytics.exportReport",
                "Export authoritative expense and life reports in CSV, JSON, or PDF format",
                ToolDomain.REPORTS,
                ToolOperationType.EXPORT,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.SELF, Set.of("USER", "ADMIN"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("format", "period")
        ));

        // --- 10. Admin Hub / System Operations ---
        register(new AssistantToolDefinition(
                "admin.systemAudit",
                "Perform system-level audit and platform statistics inspection",
                ToolDomain.ADMIN,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.SINGLE, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_1, false, false, false, false, false, false, false, false),
                List.of(),
                List.of("filter")
        ));

        register(new AssistantToolDefinition(
                "admin.bulkUserStatusUpdate",
                "Admin bulk update user account status or lock flags with mandatory reason",
                ToolDomain.ADMIN,
                ToolOperationType.UPDATE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("userIds", "status"),
                List.of("reason")
        ));

        register(new AssistantToolDefinition(
                "admin.purgeTestRecords",
                "Admin bulk purge test records across community tables with mandatory reason",
                ToolDomain.ADMIN,
                ToolOperationType.DELETE,
                new AuthorizationPolicy(ToolScope.ADMIN, OperationScope.BULK, ResourceVisibility.ADMIN_SCOPED, TargetScope.SYSTEM, Set.of("ADMIN"), false, DataSensitivity.INTERNAL),
                new OperationPolicy(ToolRiskTier.TIER_3, true, true, true, true, true, true, true, true),
                List.of("targetTable"),
                List.of("cutoffDate")
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
        if (tool.authPolicy() == null) {
            throw new IllegalStateException("Tool authPolicy must be defined for " + tool.name());
        }
        if (tool.opPolicy() == null) {
            throw new IllegalStateException("Tool opPolicy must be defined for " + tool.name());
        }
        if (tool.allowedRoles() == null || tool.allowedRoles().isEmpty()) {
            throw new IllegalStateException("Tool allowedRoles must not be empty for " + tool.name());
        }

        // Fail-Fast: READ operation must be TIER_1 and non-destructive
        if (tool.operationType() == ToolOperationType.READ || tool.operationType() == ToolOperationType.REPORT) {
            if (tool.riskTier() != ToolRiskTier.TIER_1) {
                throw new IllegalStateException("READ/REPORT operation must have TIER_1 risk classification: " + tool.name());
            }
            if (tool.confirmationRequired()) {
                throw new IllegalStateException("READ operation must not require confirmation: " + tool.name());
            }
            if (tool.destructive()) {
                throw new IllegalStateException("READ operation cannot be destructive: " + tool.name());
            }
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
        if (tool.operationScope() == OperationScope.BULK && tool.operationType() != ToolOperationType.READ) {
            if (!tool.bulkAllowed()) {
                throw new IllegalStateException("Bulk tool MUST have bulkAllowed=true: " + tool.name());
            }
            if (!tool.previewRequired()) {
                throw new IllegalStateException("Bulk mutation tool MUST have previewRequired=true: " + tool.name());
            }
        }

        // Fail-Fast: ADMIN scope tools must strictly allow only ADMIN role and require adminReason
        if (tool.scope() == ToolScope.ADMIN) {
            if (tool.allowedRoles().contains("USER") || !tool.allowedRoles().contains("ADMIN")) {
                throw new IllegalStateException("ADMIN scope tool must be restricted to ADMIN role only: " + tool.name());
            }
            if (tool.operationType() != ToolOperationType.READ && !tool.adminReasonRequired()) {
                throw new IllegalStateException("ADMIN mutation tool must have adminReasonRequired=true: " + tool.name());
            }
            // Sensitivity Guard: Admin AI cannot be configured to mutate user-private confidential/restricted records
            if (tool.visibility() == ResourceVisibility.USER_PRIVATE && (tool.dataSensitivity() == DataSensitivity.RESTRICTED || tool.dataSensitivity() == DataSensitivity.CONFIDENTIAL)) {
                throw new IllegalStateException("Admin AI cannot target USER_PRIVATE RESTRICTED/CONFIDENTIAL data: " + tool.name());
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
