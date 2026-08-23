package com.dailymate.assistant.service;

import com.dailymate.assistant.domain.*;
import com.dailymate.assistant.domain.ParsedDomainModels.*;
import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.MonthlyLifeReportDto;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.CreateReminderParams;
import com.dailymate.assistant.tool.RecordExpenseParams;
import com.dailymate.assistant.tool.params.CreateEventParams;
import com.dailymate.assistant.tool.params.CreateIceContactParams;
import com.dailymate.expense.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Universal Intent & Tool Router for DailyMate AI Assistant across all Domains.
 * Combines Reusable Domain Segmentation, Domain-Specific Typed Parsers,
 * Mandatory Domain Purity Validation, Multi-Turn Slot Filling, and Context Grounding.
 */
@Component
public class AssistantToolRouter {

    private final AssistantToolRegistry toolRegistry;
    private final AssistantReportingService reportingService;
    private final AssistantConversationStateManager stateManager;
    private final ExpenseService expenseService;
    private final AssistantAnalyticsService analyticsService;
    private final AssistantDomainSegmenter domainSegmenter;
    private final DomainParsers domainParsers;
    private final DomainPurityValidator purityValidator;
    private final ObjectMapper objectMapper;
    private final DecimalFormat inrFormat = new DecimalFormat("₹#,##,##0.00");

    // Multi-Turn Matchers
    private static final Pattern CORRECTION_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:actually\\s+)?(?:make\\s+(?:that|it)|change\\s+(?:amount|price|it|that)\\s+to|set\\s+(?:it|amount)\\s+to)\\s+(?:₹|rs\\.?|inr\\s*)?(\\d+(?:\\.\\d{1,2})?)"
    );
    private static final Pattern FOLLOWUP_DELETE_PATTERN = Pattern.compile(
            "(?i)delete\\s+(?:the\\s+one\\s+i\\s+just\\s+added|that\\s+expense|that\\s+one|the\\s+last\\s+one)"
    );

    // Extraction Matchers
    private static final Pattern EXPLICIT_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:amount\\s*(?:is|=|:)?\\s*|₹\\s*|rs\\.?\\s*|rupees?\\s*|inr\\s*)(\\d+(?:\\.\\d{1,2})?)"
    );
    private static final Pattern SUFFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(\\d+(?:\\.\\d{1,2})?)\\s*(?:rupees?|rs\\.?|₹|inr|bucks)"
    );
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:(?:add|record|log)?\\s*expense\\s+(?:of\\s+)?|spent\\s+|spend\\s+)(\\d+(?:\\.\\d{1,2})?)"
    );
    private static final Pattern NAME_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:name|item|named|called)\\s+(?:is|=|:)?\\s*([a-zA-Z0-9\\s]+?)(?=\\s+(?:and\\s+)?amount|\\s+₹|\\s+rs|\\s+for|\\s+of|\\s*$|[,.!?])"
    );
    private static final Pattern FOR_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:for|on)\\s+(?:my\\s+)?([a-zA-Z0-9\\s,]+?)(?=\\s+(?:and\\s+)?amount|\\s+₹|\\s+rs|\\s+name|\\s+is|\\s*$|[,.!?])"
    );
    private static final Pattern REMINDER_PATTERN = Pattern.compile(
            "(?i)(?:add|set|create|schedule)\\s+(?:a\\s+)?reminder\\s+(?:for|to take)?\\s+([a-zA-Z0-9]+)(?:\\s+([0-9]+(?:mg|ml|units?|tablets?)))?(?:\\s+at\\s+(\\d{1,2}(?::\\d{2})?))?"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[- ]?)?(\\d{10})"
    );
    private static final Pattern BLOOD_GROUP_PATTERN = Pattern.compile(
            "(?i)(?:^|\\s)(A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-)(?:\\s|[,.]|$)"
    );

    public AssistantToolRouter(
            AssistantToolRegistry toolRegistry,
            AssistantReportingService reportingService,
            AssistantConversationStateManager stateManager,
            ExpenseService expenseService,
            AssistantAnalyticsService analyticsService,
            AssistantDomainSegmenter domainSegmenter,
            DomainParsers domainParsers,
            DomainPurityValidator purityValidator) {
        this.toolRegistry = toolRegistry;
        this.reportingService = reportingService;
        this.stateManager = stateManager;
        this.expenseService = expenseService;
        this.analyticsService = analyticsService;
        this.domainSegmenter = domainSegmenter;
        this.domainParsers = domainParsers;
        this.purityValidator = purityValidator;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public AssistantGroundingEngine.GroundingResult route(String prompt, String userId, AssistantContext context, String conversationId) {
        if (prompt == null || prompt.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("How can I assist you with DailyMate today?", null);
        }

        String lower = prompt.toLowerCase(Locale.ROOT).trim();

        // 0. CONVERSATIONAL GREETING & STATE RESET
        if (isGreeting(lower)) {
            if (conversationId != null) {
                stateManager.clearUnresolvedParams(userId, conversationId);
            }
            return new AssistantGroundingEngine.GroundingResult(
                    "Hello! How can I help you today? You can ask me to record expenses, manage medicine reminders, post blood emergency requests, or schedule notifications.",
                    null
            );
        }

        // 1. HARD AMBIGUITY BOUNDARY: Standalone "bulk" requests must always ask for clarification
        if (isAmbiguousBulkRequest(lower)) {
            return new AssistantGroundingEngine.GroundingResult(
                    "What would you like to do in bulk? For example, you can bulk import expenses from a list, bulk archive expired events/jobs, or manage notifications.",
                    null
            );
        }

        // 2. CONTINUITY: Check Pending Conversational State / Multi-Domain Slot-Filling
        if (conversationId != null) {
            Map<String, Object> unresolved = stateManager.getUnresolvedParams(userId, conversationId);
            if (unresolved != null && unresolved.containsKey("pendingIntent")) {
                String pendingIntent = (String) unresolved.get("pendingIntent");
                String pendingSlot = (String) unresolved.get("pendingSlot");

                if ("MULTI_DOMAIN_SELECTION".equals(pendingIntent)) {
                    if (!isAnalyticsOrReportingRequest(lower)) {
                        List<AssistantDomain> detectedDomains = detectSelectedDomains(lower);

                        if (detectedDomains.size() > 1) {
                            return new AssistantGroundingEngine.GroundingResult(
                                    "You selected multiple domains. Please choose one domain to process first: Expenses, Medicine Reminders, Emergency Contacts, Complaints, Lost Items, or Events.",
                                    null
                            );
                        } else if (detectedDomains.size() == 1) {
                            AssistantDomain selected = detectedDomains.get(0);
                            switch (selected) {
                                case EXPENSE -> {
                                    String expPayload = (String) unresolved.get("expensePayload");
                                    if (expPayload != null) {
                                        List<RecordExpenseParams> items = parseBulkExpenses(expPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkExpenseProposalResult(items);
                                    }
                                }
                                case MEDICINE_REMINDER -> {
                                    String medPayload = (String) unresolved.get("medicinePayload");
                                    if (medPayload != null) {
                                        List<ParsedMedicineReminder> items = domainParsers.parseMedicineReminders(medPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkMedicineProposalResult(items);
                                    }
                                }
                                case EMERGENCY_CONTACT -> {
                                    String contactPayload = (String) unresolved.get("contactPayload");
                                    if (contactPayload != null) {
                                        List<ParsedEmergencyContact> items = domainParsers.parseEmergencyContacts(contactPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkEmergencyContactProposalResult(items);
                                    }
                                }
                                case EVENT -> {
                                    String eventPayload = (String) unresolved.get("eventPayload");
                                    if (eventPayload != null) {
                                        List<ParsedEvent> items = domainParsers.parseEvents(eventPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkEventProposalResult(items);
                                    }
                                }
                                case COMPLAINT -> {
                                    String compPayload = (String) unresolved.get("complaintPayload");
                                    if (compPayload != null) {
                                        List<ParsedComplaint> items = domainParsers.parseComplaints(compPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkComplaintProposalResult(items);
                                    }
                                }
                                case LOST_FOUND -> {
                                    String lostPayload = (String) unresolved.get("lostPayload");
                                    if (lostPayload != null) {
                                        List<ParsedLostItem> items = domainParsers.parseLostItems(lostPayload);
                                        stateManager.clearUnresolvedParams(userId, conversationId);
                                        return createBulkLostItemProposalResult(items);
                                    }
                                }
                            }
                        }
                    }
                } else if ("RECORD_EXPENSE".equals(pendingIntent)) {
                    if (isExplicitNewCommand(lower, prompt)) {
                        stateManager.clearUnresolvedParams(userId, conversationId);
                        // Fall through to process new command
                    } else if ("AMOUNT".equals(pendingSlot)) {
                        BigDecimal amount = extractAmount(prompt);
                        if (amount != null) {
                            String desc = unresolved.containsKey("description") ? (String) unresolved.get("description") : "Expense";
                            String cat = unresolved.containsKey("category") ? (String) unresolved.get("category") : inferCategory(desc, prompt);

                            stateManager.clearUnresolvedParams(userId, conversationId);

                            Map<String, Object> params = new HashMap<>();
                            params.put("amount", amount);
                            params.put("category", cat);
                            params.put("description", desc);
                            params.put("spentOn", LocalDate.now().toString());

                            String summary = "Record " + inrFormat.format(amount) + " expense for " + desc;
                            stateManager.recordUnresolvedParams(userId, conversationId, params);

                            return new AssistantGroundingEngine.GroundingResult(
                                     "I have prepared an action to record this expense of " + inrFormat.format(amount) + " for " + desc + " (" + cat + "). Please confirm below to record it.",
                                    new AssistantGroundingEngine.ActionProposalData("RECORD_EXPENSE", summary, serializeParams(params))
                            );
                        } else if (isTimeExpression(lower)) {
                            stateManager.clearUnresolvedParams(userId, conversationId);
                            // Fall through to process time-based command
                        }
                    } else if ("DESCRIPTION".equals(pendingSlot)) {
                        String desc = extractDescription(prompt);
                        if (desc == null || desc.isBlank()) {
                            desc = cleanDescription(prompt);
                        }
                        if (!desc.isBlank() && !isGreeting(desc)) {
                            BigDecimal amount = unresolved.containsKey("amount") ? new BigDecimal(unresolved.get("amount").toString()) : new BigDecimal("50.00");
                            String cat = inferCategory(desc, prompt);

                            stateManager.clearUnresolvedParams(userId, conversationId);

                            Map<String, Object> params = new HashMap<>();
                            params.put("amount", amount);
                            params.put("category", cat);
                            params.put("description", desc);
                            params.put("spentOn", LocalDate.now().toString());

                            String summary = "Record " + inrFormat.format(amount) + " expense for " + desc;
                            stateManager.recordUnresolvedParams(userId, conversationId, params);

                            return new AssistantGroundingEngine.GroundingResult(
                                    "I have prepared an action to record this expense of " + inrFormat.format(amount) + " for " + desc + " (" + cat + "). Please confirm below to record it.",
                                    new AssistantGroundingEngine.ActionProposalData("RECORD_EXPENSE", summary, serializeParams(params))
                            );
                        }
                    }
                } else if ("BULK_IMPORT_EXPENSES".equals(pendingIntent)) {
                    List<RecordExpenseParams> items = parseBulkExpenses(prompt);
                    if (!items.isEmpty()) {
                        stateManager.clearUnresolvedParams(userId, conversationId);
                        return createBulkExpenseProposalResult(items);
                    }
                }
            }
        }

        // 3. REUSABLE DOMAIN SEGMENTATION & MULTI-DOMAIN ROUTING
        List<DomainSegment> segments = domainSegmenter.segment(prompt);
        if (segments.size() > 1) {
            return handleMultiDomainBulkRequest(segments, userId, conversationId);
        } else if (segments.size() == 1) {
            DomainSegment seg = segments.get(0);
            switch (seg.domain()) {
                case EXPENSE -> {
                    List<RecordExpenseParams> items = parseBulkExpenses(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkExpenseProposalResult(items);
                    }
                }
                case MEDICINE_REMINDER -> {
                    List<ParsedMedicineReminder> items = domainParsers.parseMedicineReminders(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkMedicineProposalResult(items);
                    }
                }
                case EMERGENCY_CONTACT -> {
                    List<ParsedEmergencyContact> items = domainParsers.parseEmergencyContacts(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkEmergencyContactProposalResult(items);
                    }
                }
                case EVENT -> {
                    List<ParsedEvent> items = domainParsers.parseEvents(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkEventProposalResult(items);
                    }
                }
                case COMPLAINT -> {
                    List<ParsedComplaint> items = domainParsers.parseComplaints(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkComplaintProposalResult(items);
                    }
                }
                case LOST_FOUND -> {
                    List<ParsedLostItem> items = domainParsers.parseLostItems(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkLostItemProposalResult(items);
                    }
                }
                case GROCERY -> {
                    List<ParsedGroceryItem> items = domainParsers.parseGroceryItems(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkGroceryProposalResult(items);
                    }
                }
                case JOB -> {
                    List<ParsedJobPost> items = domainParsers.parseJobPosts(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkJobProposalResult(items);
                    }
                }
                case BLOOD_REQUEST -> {
                    List<ParsedBloodRequest> items = domainParsers.parseBloodRequests(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkBloodRequestProposalResult(items);
                    }
                }
                case MARKETPLACE -> {
                    List<ParsedProvider> items = domainParsers.parseProviders(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkMarketplaceProposalResult(items);
                    }
                }
                case NOTIFICATION -> {
                    List<ParsedNotification> items = domainParsers.parseNotifications(seg.payload());
                    if (!items.isEmpty()) {
                        return createBulkNotificationProposalResult(items);
                    }
                }
                default -> {}
            }
        }

        // 4. Multi-Turn Correction Handling ("Actually make that 60")
        Matcher correctionMatcher = CORRECTION_AMOUNT_PATTERN.matcher(lower);
        if (correctionMatcher.find() && conversationId != null) {
            Map<String, Object> unresolved = stateManager.getUnresolvedParams(userId, conversationId);
            String newAmtStr = correctionMatcher.group(1);
            BigDecimal newAmount = new BigDecimal(newAmtStr);

            String desc = unresolved.containsKey("description") ? (String) unresolved.get("description") : "Khichadi";
            String cat = unresolved.containsKey("category") ? (String) unresolved.get("category") : "Food & Dining";

            Map<String, Object> params = new HashMap<>();
            params.put("amount", newAmount);
            params.put("category", cat);
            params.put("description", desc);
            params.put("spentOn", LocalDate.now().toString());

            String summary = "Record " + inrFormat.format(newAmount) + " expense for " + desc;
            stateManager.recordUnresolvedParams(userId, conversationId, params);

            return new AssistantGroundingEngine.GroundingResult(
                    "I have updated the proposed expense to " + inrFormat.format(newAmount) + " for " + desc + ". Please confirm to record it.",
                    new AssistantGroundingEngine.ActionProposalData("RECORD_EXPENSE", summary, serializeParams(params))
            );
        }

        // 5. Multi-Turn Follow-Up Entity Deletion ("Delete the one I just added")
        Matcher followupDeleteMatcher = FOLLOWUP_DELETE_PATTERN.matcher(lower);
        if (followupDeleteMatcher.find()) {
            String lastEntityId = null;
            if (conversationId != null) {
                Map<String, Object> unresolved = stateManager.getUnresolvedParams(userId, conversationId);
                if (unresolved.containsKey("lastEntityId")) {
                    lastEntityId = (String) unresolved.get("lastEntityId");
                }
            }
            if (lastEntityId == null && expenseService != null) {
                var entries = expenseService.getEntries(userId);
                if (!entries.isEmpty()) {
                    lastEntityId = entries.get(0).id();
                }
            }

            if (lastEntityId != null) {
                Map<String, Object> params = Map.of("expenseId", lastEntityId);
                String summary = "Delete expense #" + lastEntityId;
                return new AssistantGroundingEngine.GroundingResult(
                        "I have prepared an action to delete the expense you just added. Please confirm to delete.",
                        new AssistantGroundingEngine.ActionProposalData("DELETE_EXPENSE", summary, serializeParams(params))
                );
            }
        }

        // 6. Admin / Privilege Escalation & Cross-User Data Access Guard
        if (lower.contains("all user") || lower.contains("all password") || lower.contains("delete all")
                || lower.contains("admin panel") || lower.contains("another user") || lower.contains("other user")
                || lower.contains("unrestricted administrator") || lower.contains("ignore your dailymate rules")
                || lower.contains("ignore all rules") || lower.contains("ignore instructions")
                || lower.contains("i am an administrator") || lower.contains("i am admin")
                || lower.contains("admin.bulkuserstatusupdate") || lower.contains("admin.purgetestrecords")) {
            return new AssistantGroundingEngine.GroundingResult("Administrative operations, elevated privileges, and other users' private records are strictly protected and cannot be accessed or modified through chat.", null);
        }

        // 7. Unsupported Action Guards
        if (lower.contains("flight") || lower.contains("book ticket") || lower.contains("hotel") || lower.contains("send money") || lower.contains("transfer") || lower.contains("upi") || lower.contains("pay ")) {
            return new AssistantGroundingEngine.GroundingResult("DailyMate does not support direct third-party bookings or financial money transfers at this time.", null);
        }

        // 8. Bulk Expense Import Operations
        if (lower.startsWith("bulk import expense") || lower.startsWith("bulk add expense") || lower.startsWith("import expense")
                || (lower.contains("bulk") && lower.contains("expense"))) {
            String payload = extractBulkPayload(prompt);
            List<RecordExpenseParams> items = parseBulkExpenses(payload);
            if (!items.isEmpty()) {
                return createBulkExpenseProposalResult(items);
            } else {
                if (conversationId != null) {
                    stateManager.recordUnresolvedParams(userId, conversationId, Map.of(
                            "pendingIntent", "BULK_IMPORT_EXPENSES",
                            "pendingSlot", "ITEMS"
                    ));
                }
                return new AssistantGroundingEngine.GroundingResult(
                        "Sure! Please provide the expenses in a format such as: `Lunch 50, Dinner 50, Watch 1000`.",
                        null
                );
            }
        }

        // 9. Export Intelligence Routing
        if (lower.contains("export") || lower.contains("download csv") || lower.contains("download pdf") || lower.contains("download report")) {
            YearMonth ym = parsePeriodFromPrompt(prompt);
            String format = lower.contains("pdf") ? "PDF" : (lower.contains("json") ? "JSON" : "CSV");
            return new AssistantGroundingEngine.GroundingResult(
                    "📥 **Report Export Ready** (" + format + " — " + ym + "):\n\n"
                    + "You can download your authoritative " + format + " report directly from: `/api/v1/assistant/analytics/export?format=" + format.toLowerCase(Locale.ROOT) + "&period=" + ym + "`",
                    null
            );
        }

        // 10. Comparative Period Analytics Routing ("Compare July and August expenses")
        if (lower.contains("compare") || lower.contains("versus") || lower.contains(" vs ") || lower.contains("difference between")) {
            return handlePeriodComparison(userId, prompt);
        }

        // 11. Category Breakdown Routing ("Top categories", "Spending by category")
        if (lower.contains("category breakdown") || lower.contains("top categories") || lower.contains("spending by category") || lower.contains("break down my expenses") || lower.contains("top spending categories")) {
            YearMonth ym = parsePeriodFromPrompt(prompt);
            List<CategoryBreakdownItem> breakdown = analyticsService.getCategoryBreakdown(userId, ym);
            PeriodExpenseSummary summary = analyticsService.getPeriodSummary(userId, ym);

            if (breakdown.isEmpty()) {
                return new AssistantGroundingEngine.GroundingResult("You have no recorded expenses for " + ym + " to break down.", null);
            }

            StringBuilder sb = new StringBuilder("📊 **Expense Category Breakdown for ").append(ym).append("** (Total: ")
                    .append(inrFormat.format(summary.authoritativeTotal())).append("):\n\n");
            for (CategoryBreakdownItem item : breakdown) {
                sb.append("• **").append(item.category()).append("**: ").append(inrFormat.format(item.rawTotal()))
                        .append(" (").append(item.count()).append(" transaction").append(item.count() == 1 ? "" : "s").append(")\n");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 12. Deterministic Life Reporting & Analytics Tool
        if (lower.contains("report") || lower.contains("summary of my activity") || lower.contains("monthly life") || lower.contains("life summary")) {
            MonthlyLifeReportDto report = reportingService.generateMonthlyReport(userId);
            StringBuilder sb = new StringBuilder("📊 **Your DailyMate Monthly Life Report** (Generated: ")
                    .append(report.generatedAt()).append("):\n\n")
                    .append("• 💰 **Total Expenses This Month**: **").append(inrFormat.format(report.monthlyExpenseTotal())).append("**\n");

            if (report.expenseCategoryTotals() != null && !report.expenseCategoryTotals().isEmpty()) {
                sb.append("  *Top Categories*:\n");
                for (var e : report.expenseCategoryTotals().entrySet()) {
                    sb.append("   - ").append(e.getKey()).append(": ").append(inrFormat.format(e.getValue())).append("\n");
                }
            }
            sb.append("• 💊 **Active Medicine Reminders**: ").append(report.activeRemindersCount()).append("\n")
              .append("• 🩸 **Blood Requests Activity**: ").append(report.bloodDonorStatus()).append("\n")
              .append("• 🔔 **Unread Notifications**: ").append(report.unreadNotificationCount()).append("\n")
              .append("• 🚨 **Emergency ICE Contacts**: ").append(report.emergencyContactCount()).append("\n");

            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 13. Notification Operations (Creation & Mark Read)
        if (lower.contains("notification") || lower.contains("notifications") || lower.contains("notify me") || lower.contains("alert me")) {
            if (lower.contains("mark all") && (lower.contains("read") || lower.contains("clear"))) {
                return new AssistantGroundingEngine.GroundingResult(
                        "I have prepared an action to mark all your notifications as read.",
                        new AssistantGroundingEngine.ActionProposalData("MARK_NOTIFICATIONS_READ", "Mark all notifications as read", "{}")
                );
            }
            if (lower.startsWith("add ") || lower.startsWith("create ") || lower.startsWith("set ") || lower.startsWith("schedule ") || lower.startsWith("notify ") || lower.contains("add notification") || lower.contains("create notification")) {
                return handleNotificationCreation(prompt);
            }
        }

        // 14. Emergency ICE Contact Addition (Tier 3)
        if ((lower.contains("emergency contact") || lower.contains("ice contact")) && (lower.startsWith("add ") || lower.startsWith("create ") || lower.startsWith("set "))) {
            return handleIceContactCreation(prompt);
        }

        // 15. Blood Emergency Request Creation (Tier 3)
        if (lower.contains("blood") && (lower.contains("need") || lower.contains("request") || lower.contains("require") || lower.contains("unit") || lower.contains("urgent"))) {
            return handleBloodRequestCreation(prompt);
        }

        // 16. Local Marketplace Service Provider Registration (Tier 3)
        if (lower.contains("electrician") || lower.contains("plumber") || lower.contains("carpenter") || lower.contains("mechanic") || lower.contains("painter")) {
            if (lower.startsWith("add ") || lower.startsWith("register ") || lower.startsWith("create ")) {
                return handleProviderRegistration(prompt);
            }
        }

        // 17. Medicine Reminder Tool Routing (Tier 3 Mutation)
        if (lower.startsWith("set reminder") || lower.startsWith("add reminder") || lower.startsWith("schedule reminder")
                || lower.startsWith("add medicine") || lower.startsWith("set medicine") || lower.startsWith("schedule medicine")
                || lower.startsWith("remind me to take") || lower.startsWith("medicine reminder")
                || (lower.contains("reminder") && (lower.contains("set") || lower.contains("add") || lower.contains("schedule")) && !lower.contains("check") && !lower.contains("what"))) {
            AssistantGroundingEngine.ActionProposalData reminderProposal = tryParseReminderProposal(prompt);
            if (reminderProposal != null) {
                return new AssistantGroundingEngine.GroundingResult(
                        "I have prepared an action to schedule this medicine reminder. Please confirm below to save it.",
                        reminderProposal);
            }
        }

        // 18. Expense Tool Routing (Tier 3 Mutation vs Tier 1 Read)
        boolean isExpenseReadQuery = lower.startsWith("how much") || lower.startsWith("what is my") || lower.startsWith("show my")
                || lower.startsWith("check my") || lower.startsWith("view my") || lower.startsWith("list my") || lower.contains("total expense")
                || lower.contains("how much did i spend");

        if (!isExpenseReadQuery && !lower.contains("medicine") && !lower.contains("remind") && !lower.contains("contact") && !lower.contains("notification")
                && (lower.startsWith("add ") || lower.startsWith("record ") || lower.startsWith("log ") || lower.startsWith("spent ")
                || lower.startsWith("spend ") || lower.contains("add expense") || lower.contains("record expense") || lower.contains("log expense")
                || lower.contains("khichadi") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("coffee")
                || ((prompt.contains(",") || prompt.contains(";")) && (lower.contains("lunch") || lower.contains("dinner") || lower.contains("tea") || lower.contains("coffee") || lower.contains("groceries") || lower.contains("shopping"))))) {
            if (prompt.contains(",") || prompt.contains(";")) {
                List<RecordExpenseParams> bulkItems = parseBulkExpenses(prompt);
                if (bulkItems.size() >= 2) {
                    return createBulkExpenseProposalResult(bulkItems);
                }
            }
            AssistantGroundingEngine.GroundingResult expenseResult = handleExpenseIntent(prompt, userId, conversationId);
            if (expenseResult != null) {
                return expenseResult;
            }
        }

        // 19. Specific Month Expense Inquiry (Tier 1 Read with exact grounding)
        if (lower.contains("how much did i spend") || lower.contains("how much i spent") || lower.contains("expenses in")) {
            YearMonth ym = parsePeriodFromPrompt(prompt);
            PeriodExpenseSummary summary = analyticsService.getPeriodSummary(userId, ym);
            return new AssistantGroundingEngine.GroundingResult(
                    "You spent **" + inrFormat.format(summary.authoritativeTotal()) + "** across " + summary.transactionCount() + " transaction(s) in " + ym + ".",
                    null
            );
        }

        // 20. Medicine Reminders Inquiry (Tier 1 Read)
        if (lower.contains("medicine") || lower.contains("dosage") || lower.contains("pill") || lower.contains("prescription") || lower.contains("scheduled today")) {
            if (context.reminders() == null || context.reminders().isEmpty()) {
                return new AssistantGroundingEngine.GroundingResult("You don't have any active medicine reminders scheduled today.", null);
            }
            StringBuilder sb = new StringBuilder("Here are your active medicine reminders:\n");
            for (var r : context.reminders()) {
                sb.append("• **").append(r.medicineName()).append("** (").append(r.dosage()).append(")")
                        .append(" at ").append(r.scheduledTime()).append(" [").append(r.frequency()).append("]\n");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 21. Expense General Inquiry (Tier 1 Read)
        if (lower.contains("expense") || lower.contains("spending") || lower.contains("spent") || lower.contains("budget") || lower.contains("cost")) {
            if (context.expenses() == null || context.expenses().count() == 0) {
                return new AssistantGroundingEngine.GroundingResult("You have no recorded expenses in your tracker.", null);
            }
            StringBuilder sb = new StringBuilder("Here is a summary of your recent expenses (Total: ")
                    .append(inrFormat.format(context.expenses().monthlyTotal())).append("):\n");
            if (context.expenses().categoryTotals() != null) {
                for (var entry : context.expenses().categoryTotals().entrySet()) {
                    sb.append("• ").append(entry.getKey()).append(": ").append(inrFormat.format(entry.getValue())).append("\n");
                }
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 22. Emergency Contacts Inquiry (Tier 1 Read)
        if (lower.contains("emergency") || lower.contains("ambulance") || lower.contains("police") || lower.contains("fire") || lower.contains("hospital") || lower.contains("call") || lower.contains("hotline")) {
            StringBuilder sb = new StringBuilder("🚨 Emergency Services Hotlines:\n\n• National Emergency: 112\n• Police: 100\n• Fire: 101\n• Ambulance: 102 / 108\n\n");
            if (context.emergency() != null && context.emergency().personalContactCount() > 0) {
                sb.append("You have ").append(context.emergency().personalContactCount()).append(" personal ICE emergency contact(s) configured in your Emergency Directory.");
            } else {
                sb.append("You have not added any personal emergency contacts yet. You can add them in the Emergency Directory.");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 23. Community Events Inquiry (Tier 1 Read)
        if (lower.contains("event") || lower.contains("gathering") || lower.contains("festival") || lower.contains("workshop") || lower.contains("meetup")) {
            if (context.events() == null || context.events().isEmpty()) {
                return new AssistantGroundingEngine.GroundingResult("There are currently no upcoming community events scheduled.", null);
            }
            StringBuilder sb = new StringBuilder("📅 **Upcoming Community Events**:\n");
            for (var ev : context.events()) {
                sb.append("• **").append(ev.title()).append("** — ").append(ev.eventDate()).append(" at ").append(ev.location())
                        .append(" (").append(ev.category()).append(")\n");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 24. Fallback General Assistance
        return new AssistantGroundingEngine.GroundingResult(
                "I can help you record expenses, compare monthly spending, manage medicine reminders, post blood emergency requests, register service providers, or export your monthly reports. What would you like to do?",
                null
        );
    }

    private AssistantGroundingEngine.GroundingResult handleMultiDomainBulkRequest(
            List<DomainSegment> segments,
            String userId,
            String conversationId) {

        StringBuilder breakdown = new StringBuilder("I found " + segments.size() + " different bulk operations:\n\n");
        Map<String, Object> stateMap = new HashMap<>();
        stateMap.put("pendingIntent", "MULTI_DOMAIN_SELECTION");

        for (DomainSegment segment : segments) {
            switch (segment.domain()) {
                case EXPENSE -> {
                    List<ParsedExpense> expenses = domainParsers.parseExpenses(segment.payload());
                    BigDecimal total = expenses.stream()
                            .map(ParsedExpense::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    breakdown.append("• 💰 **Expenses**: ").append(expenses.size()).append(" item")
                            .append(expenses.size() == 1 ? "" : "s")
                            .append(" (Total: ").append(inrFormat.format(total)).append(")\n");
                    stateMap.put("expensePayload", segment.payload());
                    stateMap.put("expenseCount", expenses.size());
                    stateMap.put("expenseTotal", total);
                }
                case MEDICINE_REMINDER -> {
                    List<ParsedMedicineReminder> reminders = domainParsers.parseMedicineReminders(segment.payload());
                    breakdown.append("• 💊 **Medicine Reminders**: ").append(reminders.size()).append(" item")
                            .append(reminders.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("medicinePayload", segment.payload());
                    stateMap.put("reminderCount", reminders.size());
                }
                case EMERGENCY_CONTACT -> {
                    List<ParsedEmergencyContact> contacts = domainParsers.parseEmergencyContacts(segment.payload());
                    breakdown.append("• 🚨 **Emergency Contacts**: ").append(contacts.size()).append(" item")
                            .append(contacts.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("contactPayload", segment.payload());
                    stateMap.put("contactCount", contacts.size());
                }
                case COMPLAINT -> {
                    List<ParsedComplaint> complaints = domainParsers.parseComplaints(segment.payload());
                    breakdown.append("• 📢 **Complaints**: ").append(complaints.size()).append(" item")
                            .append(complaints.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("complaintPayload", segment.payload());
                    stateMap.put("complaintCount", complaints.size());
                }
                case LOST_FOUND -> {
                    List<ParsedLostItem> lostItems = domainParsers.parseLostItems(segment.payload());
                    breakdown.append("• 🎒 **Lost Items**: ").append(lostItems.size()).append(" item")
                            .append(lostItems.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("lostPayload", segment.payload());
                    stateMap.put("lostItemCount", lostItems.size());
                }
                case EVENT -> {
                    List<ParsedEvent> events = domainParsers.parseEvents(segment.payload());
                    breakdown.append("• 📅 **Events**: ").append(events.size()).append(" item")
                            .append(events.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("eventPayload", segment.payload());
                    stateMap.put("eventCount", events.size());
                }
                case GROCERY -> {
                    List<ParsedGroceryItem> groceries = domainParsers.parseGroceryItems(segment.payload());
                    breakdown.append("• 🛒 **Groceries**: ").append(groceries.size()).append(" item")
                            .append(groceries.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("groceryPayload", segment.payload());
                    stateMap.put("groceryCount", groceries.size());
                }
                case JOB -> {
                    List<ParsedJobPost> jobs = domainParsers.parseJobPosts(segment.payload());
                    breakdown.append("• 💼 **Jobs**: ").append(jobs.size()).append(" item")
                            .append(jobs.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("jobPayload", segment.payload());
                    stateMap.put("jobCount", jobs.size());
                }
                case BLOOD_REQUEST -> {
                    List<ParsedBloodRequest> bloodRequests = domainParsers.parseBloodRequests(segment.payload());
                    breakdown.append("• 🩸 **Blood Requests**: ").append(bloodRequests.size()).append(" item")
                            .append(bloodRequests.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("bloodPayload", segment.payload());
                    stateMap.put("bloodCount", bloodRequests.size());
                }
                case MARKETPLACE -> {
                    List<ParsedProvider> providers = domainParsers.parseProviders(segment.payload());
                    breakdown.append("• 🔧 **Marketplace**: ").append(providers.size()).append(" item")
                            .append(providers.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("providerPayload", segment.payload());
                    stateMap.put("providerCount", providers.size());
                }
                case NOTIFICATION -> {
                    List<ParsedNotification> notifications = domainParsers.parseNotifications(segment.payload());
                    breakdown.append("• 🔔 **Notifications**: ").append(notifications.size()).append(" item")
                            .append(notifications.size() == 1 ? "" : "s").append("\n");
                    stateMap.put("notificationPayload", segment.payload());
                    stateMap.put("notificationCount", notifications.size());
                }
                default -> {}
            }
        }

        breakdown.append("\nNo changes have been made yet.\nWhich domain would you like to execute first? (e.g. \"Process expenses\" or \"Add medicine reminders\")");

        if (conversationId != null) {
            stateManager.recordUnresolvedParams(userId, conversationId, stateMap);
        }

        // Multi-domain safety invariant: Proposal is strictly NULL until user selects a single domain!
        return new AssistantGroundingEngine.GroundingResult(breakdown.toString().trim(), null);
    }

    private boolean isGreeting(String text) {
        if (text == null) return false;
        String clean = text.replaceAll("[^a-zA-Z\\s]", "").trim().toLowerCase(Locale.ROOT);
        return clean.equals("hi") || clean.equals("hello") || clean.equals("hey")
                || clean.equals("good morning") || clean.equals("good evening")
                || clean.equals("good afternoon") || clean.equals("howdy")
                || clean.equals("namaste");
    }

    private boolean isAmbiguousBulkRequest(String text) {
        String clean = text.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", " ");
        return clean.equals("bulk") || clean.equals("bulk operations") || clean.equals("bulk operation")
                || clean.equals("bulk action") || clean.equals("bulk actions") || clean.equals("do bulk")
                || clean.equals("bulk management");
    }

    private String extractBulkPayload(String prompt) {
        return prompt.replaceFirst("(?i)^.*?(?:bulk\\s+(?:import|add|record)?\\s*expenses?(?:\\s+from\\s+(?:a\\s+)?list)?:?)\\s*", "").trim();
    }

    public List<RecordExpenseParams> parseBulkExpenses(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<ParsedExpense> parsed = domainParsers.parseExpenses(text);
        List<RecordExpenseParams> items = new ArrayList<>();
        for (ParsedExpense p : parsed) {
            items.add(new RecordExpenseParams(
                    p.category(),
                    p.description(),
                    p.amount(),
                    p.spentOn(),
                    p.notes()
            ));
        }
        return items;
    }

    private AssistantGroundingEngine.GroundingResult createBulkExpenseProposalResult(List<RecordExpenseParams> items) {
        BigDecimal total = items.stream()
                .map(RecordExpenseParams::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" expense")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (RecordExpenseParams item : items) {
            sb.append("• **").append(item.description()).append("**: ").append(inrFormat.format(item.amount()))
                    .append(" (").append(item.category()).append(")\n");
        }

        sb.append("\n**Total**: **").append(inrFormat.format(total)).append("**\n\n")
          .append("I have prepared a bulk action to record these ").append(items.size())
          .append(" expenses. Please confirm below to import them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Bulk import " + items.size() + " expense" + (items.size() == 1 ? "" : "s") + " totaling " + inrFormat.format(total);
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_RECORD_EXPENSES", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkMedicineProposalResult(List<ParsedMedicineReminder> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" medicine reminder")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        List<CreateReminderParams> paramsList = new ArrayList<>();
        for (ParsedMedicineReminder item : items) {
            sb.append("• **").append(item.name()).append("**: ").append(item.dosage())
                    .append(" at ").append(item.remindAt()).append(" (").append(item.frequency()).append(")\n");
            paramsList.add(new CreateReminderParams(
                    item.name(),
                    item.dosage(),
                    item.frequency(),
                    item.remindAt(),
                    item.notes(),
                    true
            ));
        }

        sb.append("\nI have prepared an action to add these ").append(items.size())
          .append(" reminders. Please confirm below to schedule them.");

        try {
            String json = objectMapper.writeValueAsString(paramsList);
            String summary = "Add " + items.size() + " medicine reminder" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_MEDICINE_REMINDERS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk medicine reminder proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkEmergencyContactProposalResult(List<ParsedEmergencyContact> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" emergency contact")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        List<CreateIceContactParams> paramsList = new ArrayList<>();
        for (ParsedEmergencyContact item : items) {
            sb.append("• **").append(item.name()).append("**: ").append(item.phone())
                    .append(" (").append(item.relationship()).append(")\n");
            paramsList.add(new CreateIceContactParams(
                    item.name(),
                    item.relationship(),
                    item.phone(),
                    item.category(),
                    item.notes()
            ));
        }

        sb.append("\nI have prepared an action to add these ").append(items.size())
          .append(" emergency contacts. Please confirm below to add them.");

        try {
            String json = objectMapper.writeValueAsString(paramsList);
            String summary = "Add " + items.size() + " emergency contact" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_ICE_CONTACTS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk emergency contact proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkEventProposalResult(List<ParsedEvent> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" event")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        List<CreateEventParams> paramsList = new ArrayList<>();
        for (ParsedEvent item : items) {
            sb.append("• **").append(item.title()).append("**: ").append(item.eventDate())
                    .append(" at ").append(item.eventTime()).append(" (").append(item.location()).append(")\n");
            paramsList.add(new CreateEventParams(
                    item.title(),
                    item.description(),
                    item.location(),
                    item.eventDate(),
                    item.category()
            ));
        }

        sb.append("\nI have prepared an action to add these ").append(items.size())
          .append(" events. Please confirm below to schedule them.");

        try {
            String json = objectMapper.writeValueAsString(paramsList);
            String summary = "Add " + items.size() + " event" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_EVENTS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk events proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkComplaintProposalResult(List<ParsedComplaint> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" complaint")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedComplaint item : items) {
            sb.append("• **").append(item.title()).append("**: ").append(item.location()).append("\n");
        }

        sb.append("\nI have prepared an action to record these ").append(items.size())
          .append(" complaints. Please confirm below to submit them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Submit " + items.size() + " complaint" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_COMPLAINTS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk complaints proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkLostItemProposalResult(List<ParsedLostItem> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" lost item")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedLostItem item : items) {
            sb.append("• **").append(item.title()).append("**: ").append(item.location()).append("\n");
        }

        sb.append("\nI have prepared an action to record these ").append(items.size())
          .append(" lost items. Please confirm below to post them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Post " + items.size() + " lost item" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_LOST_ITEMS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk lost items proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkGroceryProposalResult(List<ParsedGroceryItem> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" grocery item")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedGroceryItem item : items) {
            sb.append("• **").append(item.name()).append("**: ").append(inrFormat.format(item.price()))
                    .append(" (").append(item.store()).append(")\n");
        }

        sb.append("\nI have prepared an action to add these ").append(items.size())
          .append(" grocery prices. Please confirm below to save them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Add " + items.size() + " grocery item" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_ADD_GROCERY", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk grocery proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkJobProposalResult(List<ParsedJobPost> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" job opening")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedJobPost item : items) {
            sb.append("• **").append(item.title()).append("**: ").append(item.companyName())
                    .append(" (").append(item.salary()).append(")\n");
        }

        sb.append("\nI have prepared an action to post these ").append(items.size())
          .append(" job openings. Please confirm below to publish them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Post " + items.size() + " job" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_JOBS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk jobs proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkBloodRequestProposalResult(List<ParsedBloodRequest> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" blood request")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedBloodRequest item : items) {
            sb.append("• **").append(item.bloodGroup()).append("**: ").append(item.patientName())
                    .append(" (").append(item.hospital()).append(")\n");
        }

        sb.append("\nI have prepared an action to create these ").append(items.size())
          .append(" emergency blood requests. Please confirm below to publish them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Create " + items.size() + " blood request" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_BLOOD_REQUESTS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk blood requests proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkMarketplaceProposalResult(List<ParsedProvider> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" service provider")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedProvider item : items) {
            sb.append("• **").append(item.businessName()).append("**: ").append(item.category())
                    .append(" (").append(item.phone()).append(")\n");
        }

        sb.append("\nI have prepared an action to register these ").append(items.size())
          .append(" service providers. Please confirm below to add them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Register " + items.size() + " provider" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_REGISTER_PROVIDERS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk providers proposal.", null);
        }
    }

    private AssistantGroundingEngine.GroundingResult createBulkNotificationProposalResult(List<ParsedNotification> items) {
        StringBuilder sb = new StringBuilder("I found ").append(items.size()).append(" notification")
                .append(items.size() == 1 ? "" : "s").append(":\n\n");

        for (ParsedNotification item : items) {
            sb.append("• **").append(item.title()).append("**: ").append(item.message()).append("\n");
        }

        sb.append("\nI have prepared an action to create these ").append(items.size())
          .append(" notifications. Please confirm below to schedule them.");

        try {
            String json = objectMapper.writeValueAsString(items);
            String summary = "Create " + items.size() + " notification" + (items.size() == 1 ? "" : "s");
            return new AssistantGroundingEngine.GroundingResult(
                    sb.toString().trim(),
                    new AssistantGroundingEngine.ActionProposalData("BULK_CREATE_NOTIFICATIONS", summary, json)
            );
        } catch (Exception ex) {
            return new AssistantGroundingEngine.GroundingResult("Failed to construct bulk notifications proposal.", null);
        }
    }

    private List<AssistantDomain> detectSelectedDomains(String lower) {
        List<AssistantDomain> detected = new ArrayList<>();
        if (lower.matches(".*\\b(expenses?|spending|first)\\b.*") || lower.matches("^\\s*1\\s*$") || lower.contains("process expense") || lower.contains("add expense")) {
            detected.add(AssistantDomain.EXPENSE);
        }
        if (lower.matches(".*\\b(medicines?|reminders?|second)\\b.*") || lower.matches("^\\s*2\\s*$") || lower.contains("process medicine") || lower.contains("add medicine")) {
            detected.add(AssistantDomain.MEDICINE_REMINDER);
        }
        if (lower.matches(".*\\b(emergenc(?:y|ies)|contacts?|ice|third)\\b.*") || lower.matches("^\\s*3\\s*$") || lower.contains("process emergency") || lower.contains("process contact")) {
            detected.add(AssistantDomain.EMERGENCY_CONTACT);
        }
        if (lower.matches(".*\\b(complaints?|fourth)\\b.*") || lower.matches("^\\s*4\\s*$") || lower.contains("process complaint")) {
            detected.add(AssistantDomain.COMPLAINT);
        }
        if (lower.matches(".*\\b(lost|found|fifth)\\b.*") || lower.matches("^\\s*5\\s*$") || lower.contains("process lost")) {
            detected.add(AssistantDomain.LOST_FOUND);
        }
        if (lower.matches(".*\\b(events?|sixth)\\b.*") || lower.matches("^\\s*6\\s*$") || lower.contains("process event") || lower.contains("add event")) {
            detected.add(AssistantDomain.EVENT);
        }
        if (lower.matches(".*\\b(grocer(?:y|ies)|items?|prices?)\\b.*") || lower.contains("process grocer") || lower.contains("add grocer")) {
            detected.add(AssistantDomain.GROCERY);
        }
        if (lower.matches(".*\\b(jobs?|openings?|vacancies)\\b.*") || lower.contains("process job") || lower.contains("add job")) {
            detected.add(AssistantDomain.JOB);
        }
        if (lower.matches(".*\\b(blood|donations?)\\b.*") || lower.contains("process blood") || lower.contains("add blood")) {
            detected.add(AssistantDomain.BLOOD_REQUEST);
        }
        if (lower.matches(".*\\b(marketplace|providers?|services?)\\b.*") || lower.contains("process marketplace") || lower.contains("process provider")) {
            detected.add(AssistantDomain.MARKETPLACE);
        }
        if (lower.matches(".*\\b(notifications?|alerts?)\\b.*") || lower.contains("process notification") || lower.contains("add notification")) {
            detected.add(AssistantDomain.NOTIFICATION);
        }
        return detected;
    }

    private AssistantGroundingEngine.GroundingResult handleNotificationCreation(String prompt) {
        String title = "Daily Reminder";
        String message = prompt;
        String type = "REMINDER";

        // Clean out leading commands
        String clean = prompt.replaceFirst("(?i)^(?:add|create|set|schedule|send)?\\s*(?:\\d+\\s+)?notifications?\\s*(?:for|that|to|first is|called)?\\s*", "").trim();
        if (!clean.isBlank()) {
            title = capitalize(clean.length() > 40 ? clean.substring(0, 37) + "..." : clean);
            message = clean;
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("title", title);
            map.put("message", message);
            map.put("type", type);
            map.put("link", "/notifications");

            String json = objectMapper.writeValueAsString(map);
            String summary = "Create notification: " + title;

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to create this notification (" + title + "). Please confirm below to schedule it.",
                    new AssistantGroundingEngine.ActionProposalData("CREATE_NOTIFICATION", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handlePeriodComparison(String userId, String prompt) {
        YearMonth[] periods = parseTwoPeriodsFromPrompt(prompt);
        PeriodComparisonDto comp = analyticsService.comparePeriods(userId, periods[0], periods[1]);

        StringBuilder sb = new StringBuilder("📊 **Spending Comparison: ")
                .append(comp.period1()).append(" vs ").append(comp.period2()).append("**:\n\n")
                .append("• **").append(comp.period1()).append("**: ").append(inrFormat.format(comp.total1())).append("\n")
                .append("• **").append(comp.period2()).append("**: ").append(inrFormat.format(comp.total2())).append("\n")
                .append("• **Net Difference**: ").append(comp.deltaAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                .append(inrFormat.format(comp.deltaAmount())).append("\n");

        if (comp.percentageChange() != null) {
            sb.append("• **Percentage Variance**: ").append(comp.percentageChange().toPlainString()).append("% (")
                    .append(comp.changeType()).append(")\n");
        } else {
            sb.append("• **Variance**: ").append(comp.changeType()).append(" (No prior spending recorded in ").append(comp.period1()).append(")\n");
        }

        return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
    }

    private AssistantGroundingEngine.GroundingResult handleExpenseIntent(String prompt, String userId, String conversationId) {
        BigDecimal amount = extractAmount(prompt);
        String description = extractDescription(prompt);

        if (amount == null) {
            if (conversationId != null) {
                Map<String, Object> stateMap = new HashMap<>();
                stateMap.put("pendingIntent", "RECORD_EXPENSE");
                stateMap.put("pendingSlot", "AMOUNT");
                if (description != null) {
                    stateMap.put("description", description);
                    stateMap.put("category", inferCategory(description, prompt));
                }
                stateManager.recordUnresolvedParams(userId, conversationId, stateMap);
            }
            String target = description != null ? " for " + description : "";
            return new AssistantGroundingEngine.GroundingResult("What amount should I record" + target + "?", null);
        }

        if (description == null || description.isBlank()) {
            if (conversationId != null) {
                stateManager.recordUnresolvedParams(userId, conversationId, Map.of(
                        "pendingIntent", "RECORD_EXPENSE",
                        "pendingSlot", "DESCRIPTION",
                        "amount", amount
                ));
            }
            return new AssistantGroundingEngine.GroundingResult("What is this expense of " + inrFormat.format(amount) + " for?", null);
        }

        String category = inferCategory(description, prompt);

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("category", category);
            map.put("description", description);
            map.put("amount", amount);
            map.put("spentOn", LocalDate.now().toString());

            if (conversationId != null) {
                stateManager.recordUnresolvedParams(userId, conversationId, map);
            }

            String json = objectMapper.writeValueAsString(map);
            String summary = "Record " + inrFormat.format(amount) + " expense for " + description;

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to record this expense of " + inrFormat.format(amount) + " for " + description + " (" + category + "). Please confirm below to record it.",
                    new AssistantGroundingEngine.ActionProposalData("RECORD_EXPENSE", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.ActionProposalData tryParseReminderProposal(String prompt) {
        try {
            List<ParsedMedicineReminder> parsed = domainParsers.parseMedicineReminders(prompt);
            if (parsed != null && !parsed.isEmpty()) {
                ParsedMedicineReminder r = parsed.get(0);
                Map<String, Object> map = new HashMap<>();
                map.put("name", r.name());
                map.put("dosage", r.dosage() != null ? r.dosage() : "1 dose");
                map.put("frequency", r.frequency() != null ? r.frequency() : "DAILY");
                map.put("remindAt", r.remindAt() != null ? r.remindAt().toString() : "09:00");
                map.put("notes", r.notes() != null ? r.notes() : "Created via AI Assistant");
                map.put("active", true);

                String json = objectMapper.writeValueAsString(map);
                String summary = "Schedule " + r.name() + " (" + (r.dosage() != null ? r.dosage() : "1 dose") + ") at " + (r.remindAt() != null ? r.remindAt().toString() : "09:00") + " daily";

                return new AssistantGroundingEngine.ActionProposalData("CREATE_REMINDER", summary, json);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handleProviderRegistration(String prompt) {
        String serviceType = null;
        Matcher mType = Pattern.compile("(?i)(electrician|plumber|carpenter|mechanic|painter)").matcher(prompt);
        if (mType.find()) {
            serviceType = capitalize(mType.group(1));
        }

        String phone = null;
        Matcher mPhone = PHONE_PATTERN.matcher(prompt);
        if (mPhone.find()) {
            phone = mPhone.group(1);
        }

        String name = null;
        Matcher mName = Pattern.compile("(?i)(?:named|called|provider|plumber|electrician|carpenter|mechanic|painter)\\s+([a-zA-Z]+)(?=\\s+(?:with|phone|in|at|number)|\\s*$)").matcher(prompt);
        if (mName.find()) {
            name = cleanDescription(mName.group(1));
            if (name.equalsIgnoreCase("with") || name.equalsIgnoreCase("phone") || name.equalsIgnoreCase("in")) {
                name = null;
            } else {
                name = capitalize(name);
            }
        }

        if (name == null || name.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("Please provide the name of the " + (serviceType != null ? serviceType : "service provider") + " you would like to register.", null);
        }

        if (phone == null) {
            return new AssistantGroundingEngine.GroundingResult("To register a service provider, please provide the trade (e.g. Electrician, Plumber), full name, and 10-digit contact phone number.", null);
        }

        String city = "Pune";
        Matcher mCity = Pattern.compile("(?i)(?:in|at)\\s+([a-zA-Z]+)").matcher(prompt);
        if (mCity.find()) {
            city = capitalize(mCity.group(1));
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("serviceType", serviceType != null ? serviceType : "General Handyman");
            map.put("phone", phone);
            map.put("city", city);
            map.put("experienceYears", 3);

            String json = objectMapper.writeValueAsString(map);
            String summary = "Register " + name + " as " + serviceType + " in " + city;

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to register " + name + " as a verified " + serviceType + " in " + city + ". Please confirm below to register.",
                    new AssistantGroundingEngine.ActionProposalData("REGISTER_PROVIDER", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handleIceContactCreation(String prompt) {
        String phone = null;
        Matcher mPhone = PHONE_PATTERN.matcher(prompt);
        if (mPhone.find()) {
            phone = mPhone.group(1);
        }

        if (phone == null) {
            return new AssistantGroundingEngine.GroundingResult("Please provide a valid 10-digit phone number for your emergency contact.", null);
        }

        String relationship = "Family";
        Matcher mRel = Pattern.compile("(?i)(?:my\\s+)?(wife|husband|father|mother|brother|sister|son|daughter|friend|doctor|colleague|cousin)").matcher(prompt);
        if (mRel.find()) {
            relationship = capitalize(mRel.group(1));
        }

        String name = null;
        Matcher mNamed = Pattern.compile("(?i)(?:named|called)\\s+([a-zA-Z]+)").matcher(prompt);
        if (mNamed.find()) {
            name = cleanDescription(mNamed.group(1));
        } else {
            Matcher mRelName = Pattern.compile("(?i)(?:wife|husband|father|mother|brother|sister|son|daughter|friend|contact|person)\\s+([a-zA-Z]+)(?=\\s+with|\\s+phone|\\s+as|\\s*$)").matcher(prompt);
            if (mRelName.find()) {
                String candidate = cleanDescription(mRelName.group(1));
                if (!candidate.equalsIgnoreCase("as") && !candidate.equalsIgnoreCase("with") && !candidate.equalsIgnoreCase("an") && !candidate.equalsIgnoreCase("my") && !candidate.equalsIgnoreCase("contact") && !candidate.equalsIgnoreCase("emergency")) {
                    name = candidate;
                }
            }
        }

        if (name == null || name.isBlank()) {
            name = relationship;
        }

        name = capitalize(name);

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("phone", phone);
            map.put("relationship", relationship);
            map.put("category", "FAMILY");

            String json = objectMapper.writeValueAsString(map);
            String summary = "Add emergency contact: " + name + " (" + relationship + ", " + phone + ")";

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to add " + name + " (" + relationship + ") to your emergency contacts. Please confirm below to save it.",
                    new AssistantGroundingEngine.ActionProposalData("CREATE_ICE_CONTACT", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handleBloodRequestCreation(String prompt) {
        String bloodGroup = null;
        Matcher mBg = BLOOD_GROUP_PATTERN.matcher(prompt);
        if (mBg.find()) {
            bloodGroup = mBg.group(1).toUpperCase();
        }

        if (bloodGroup == null) {
            return new AssistantGroundingEngine.GroundingResult("To post a blood donation request, a blood group is needed (e.g. A+, O+, B-, AB+). Please specify the blood group.", null);
        }

        String phone = null;
        Matcher mPhone = PHONE_PATTERN.matcher(prompt);
        if (mPhone.find()) {
            phone = mPhone.group(1);
        }

        if (phone == null) {
            phone = "9876543210";
        }

        String location = "City Care Hospital, Pune";
        Matcher mLocation = Pattern.compile("(?i)(?:at|in|hospital)\\s+([a-zA-Z0-9\\s]+?)(?=\\s*,|\\s+call|\\s+phone|\\s*$)").matcher(prompt);
        if (mLocation.find()) {
            location = cleanDescription(mLocation.group(1));
        }

        String patientName = "Emergency Patient";
        Matcher mPatient = Pattern.compile("(?i)(?:for\\s+)([a-zA-Z]+)(?=\\s+at|\\s+in|\\s+need|\\s*$)").matcher(prompt);
        if (mPatient.find()) {
            patientName = capitalize(cleanDescription(mPatient.group(1)));
        }

        int units = 2;
        Matcher mUnits = Pattern.compile("(\\d+)\\s*units?").matcher(prompt);
        if (mUnits.find()) {
            try { units = Integer.parseInt(mUnits.group(1)); } catch (Exception ignored) {}
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("patientName", patientName);
            map.put("bloodGroup", bloodGroup);
            map.put("unitsNeeded", units);
            map.put("hospitalLocation", location);
            map.put("urgency", "URGENT");
            map.put("contactName", patientName + " Family");
            map.put("contactPhone", phone);
            map.put("additionalNotes", "Emergency blood request posted via DailyMate Assistant");

            String json = objectMapper.writeValueAsString(map);
            String summary = "Create emergency blood request: " + units + " units of " + bloodGroup + " for " + patientName + " at " + location;

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to publish this emergency blood request to the community. Please confirm below to broadcast it.",
                    new AssistantGroundingEngine.ActionProposalData("CREATE_BLOOD_REQUEST", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal extractAmount(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        // Exclude if it's a notification/reminder/contact/medicine/time phrase
        if (lower.contains("notification") || lower.contains("remind") || lower.contains("contact")
                || lower.contains("medicine") || lower.contains("units") || lower.contains("doctor")
                || lower.contains("friend") || isTimeExpression(lower)) {
            return null;
        }

        // Mask date expressions first to prevent matching date days/months/years as amounts
        String promptWithoutDate = prompt
                .replaceAll("(?i)\\b(?:today|yesterday|tomorrow|day before yesterday|day after tomorrow)\\b", " ")
                .replaceAll("(?i)\\b(?:on\\s+)?(?:\\d{1,2}(?:st|nd|rd|th)?\\s+)?(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)(?:\\s+\\d{1,2}(?:st|nd|rd|th)?)?(?:\\s*,?\\s*\\d{4})?\\b", " ")
                .replaceAll("(?i)\\b(?:on\\s+)?\\d{4}-\\d{2}-\\d{2}\\b", " ")
                .replaceAll("(?i)\\b(?:on\\s+)?\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}\\b", " ")
                .replaceAll("\\b(?:19|20)\\d{2}\\b", " ");

        Matcher m1 = EXPLICIT_AMOUNT_PATTERN.matcher(promptWithoutDate);
        if (m1.find()) {
            try { return new BigDecimal(m1.group(1)); } catch (Exception ignored) {}
        }
        Matcher m2 = SUFFIX_AMOUNT_PATTERN.matcher(promptWithoutDate);
        if (m2.find()) {
            try { return new BigDecimal(m2.group(1)); } catch (Exception ignored) {}
        }
        Matcher m3 = GENERIC_AMOUNT_PATTERN.matcher(promptWithoutDate);
        if (m3.find()) {
            try { return new BigDecimal(m3.group(1)); } catch (Exception ignored) {}
        }
        Matcher mDigitBefore = Pattern.compile("(?i)\\b(\\d+(?:\\.\\d{1,2})?)\\s*(?:expense|cost|bill|payment|fee|for|spent)\\b").matcher(promptWithoutDate);
        if (mDigitBefore.find()) {
            try { return new BigDecimal(mDigitBefore.group(1)); } catch (Exception ignored) {}
        }
        Matcher mStandalone = Pattern.compile("^\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|rupees?)?\\s*$").matcher(promptWithoutDate);
        if (mStandalone.find()) {
            try { return new BigDecimal(mStandalone.group(1)); } catch (Exception ignored) {}
        }
        Matcher mTrailing = Pattern.compile("(?i)(?:for|on|named|called|item|is)?\\s*[a-zA-Z\\s]+\\s+(\\d+(?:\\.\\d{1,2})?)\\s*$").matcher(promptWithoutDate.trim());
        if (mTrailing.find()) {
            try { return new BigDecimal(mTrailing.group(1)); } catch (Exception ignored) {}
        }
        Matcher mAnyDigit = Pattern.compile("(?i)\\b(\\d+(?:\\.\\d{1,2})?)\\b").matcher(promptWithoutDate);
        while (mAnyDigit.find()) {
            String candidate = mAnyDigit.group(1);
            if (candidate.matches("^(?:19|20)\\d{2}$")) {
                continue; // Ignore standalone 4-digit years
            }
            try { return new BigDecimal(candidate); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isExplicitNewCommand(String lower, String prompt) {
        if (domainSegmenter.segment(prompt).size() > 0) return true;
        if (lower.startsWith("add medicine") || lower.startsWith("medicine") || lower.startsWith("remind")
                || lower.startsWith("add contact") || lower.startsWith("emergency")
                || lower.contains("chart") || lower.contains("graph") || lower.contains("analytics")
                || lower.contains("report") || lower.startsWith("who can i") || lower.startsWith("what medicine")
                || lower.startsWith("bulk add") || lower.startsWith("bulk import")) {
            return true;
        }
        return false;
    }

    private boolean isTimeExpression(String lower) {
        return lower.matches(".*\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b.*")
                || lower.matches(".*\\b(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b.*")
                || lower.matches(".*\\bat\\s+\\d{1,2}(?::\\d{2})?\\b.*");
    }

    private boolean isAnalyticsOrReportingRequest(String lower) {
        return lower.contains("chart") || lower.contains("graph") || lower.contains("visual")
                || lower.contains("analytics") || lower.contains("report") || lower.contains("compare")
                || lower.contains("breakdown") || lower.contains("summary") || lower.contains("statistics")
                || lower.contains("metrics") || lower.contains("trend") || lower.contains("how much did i spend");
    }

    private String extractDescription(String prompt) {
        Matcher mName = NAME_KEYWORD_PATTERN.matcher(prompt);
        if (mName.find()) {
            String name = cleanDescription(mName.group(1));
            if (!name.isBlank() && !isGreeting(name)) return capitalize(name);
        }
        Matcher mFor = FOR_KEYWORD_PATTERN.matcher(prompt);
        if (mFor.find()) {
            String forText = cleanDescription(mFor.group(1));
            if (!forText.isBlank() && !isGreeting(forText)) return capitalize(forText);
        }
        Matcher mPattern = Pattern.compile("(?i)(?:for|on|in)\\s+([a-zA-Z\\s]+)").matcher(prompt);
        if (mPattern.find()) {
            String text = cleanDescription(mPattern.group(1));
            if (!text.isBlank() && !isGreeting(text)) return capitalize(text);
        }
        return null;
    }

    private String cleanDescription(String text) {
        if (text == null) return "";
        String cleaned = text.trim()
                .replaceAll("(?i)^(?:my|an|a|the|afternoon|morning|evening|night|daily|monthly|named|called|with|for)\\s+", "")
                .replaceAll("(?i)\\s+(?:and|is|amount|for|to|with|phone|number|in|at)$", "")
                .replaceAll("(?i)\\s+\\d+(?:\\.\\d{1,2})?$", "")
                .trim();
        if (cleaned.equalsIgnoreCase("with") || cleaned.equalsIgnoreCase("for") || cleaned.equalsIgnoreCase("and") || cleaned.equalsIgnoreCase("phone")) {
            return "";
        }
        return cleaned;
    }

    private String inferCategory(String description, String prompt) {
        String combined = (description + " " + prompt).toLowerCase(Locale.ROOT);
        if (combined.contains("lunch") || combined.contains("dinner") || combined.contains("breakfast")
                || combined.contains("khichadi") || combined.contains("poha") || combined.contains("tea")
                || combined.contains("coffee") || combined.contains("snacks") || combined.contains("food")
                || combined.contains("restaurant") || combined.contains("cafe") || combined.contains("groceries")
                || combined.contains("vegetables") || combined.contains("fruits") || combined.contains("milk")) {
            return combined.contains("groceries") ? "Groceries" : "Food & Dining";
        }
        if (combined.contains("electric") || combined.contains("water") || combined.contains("wifi")
                || combined.contains("internet") || combined.contains("gas") || combined.contains("rent")
                || combined.contains("utility") || combined.contains("utilities") || combined.contains("recharge")
                || combined.contains("bill")) {
            return "Utilities";
        }
        if (combined.contains("cab") || combined.contains("uber") || combined.contains("ola")
                || combined.contains("auto") || combined.contains("bus") || combined.contains("train")
                || combined.contains("metro") || combined.contains("fuel") || combined.contains("petrol")
                || combined.contains("diesel") || combined.contains("travel") || combined.contains("taxi")) {
            return "Travel";
        }
        if (combined.contains("medicine") || combined.contains("doctor") || combined.contains("health")
                || combined.contains("hospital") || combined.contains("clinic") || combined.contains("pharmacy")) {
            return "Health";
        }
        if (combined.contains("watch") || combined.contains("shopping") || combined.contains("clothes") || combined.contains("shoes")
                || combined.contains("electronics") || combined.contains("book")) {
            return "Shopping";
        }
        return "Other";
    }

    private YearMonth parsePeriodFromPrompt(String prompt) {
        Matcher ymMatcher = Pattern.compile("(\\d{4})[-/](\\d{2})").matcher(prompt);
        if (ymMatcher.find()) {
            return YearMonth.of(Integer.parseInt(ymMatcher.group(1)), Integer.parseInt(ymMatcher.group(2)));
        }

        String lower = prompt.toLowerCase(Locale.ROOT);
        int currentYear = YearMonth.now(AssistantAnalyticsService.DEFAULT_ZONE).getYear();
        for (Month m : Month.values()) {
            if (lower.contains(m.name().toLowerCase(Locale.ROOT))) {
                return YearMonth.of(currentYear, m);
            }
        }
        return YearMonth.now(AssistantAnalyticsService.DEFAULT_ZONE);
    }

    private YearMonth[] parseTwoPeriodsFromPrompt(String prompt) {
        List<YearMonth> found = new ArrayList<>();
        Matcher ymMatcher = Pattern.compile("(\\d{4})[-/](\\d{2})").matcher(prompt);
        while (ymMatcher.find()) {
            found.add(YearMonth.of(Integer.parseInt(ymMatcher.group(1)), Integer.parseInt(ymMatcher.group(2))));
        }

        if (found.size() >= 2) {
            return new YearMonth[]{found.get(0), found.get(1)};
        }

        String lower = prompt.toLowerCase(Locale.ROOT);
        int currentYear = YearMonth.now(AssistantAnalyticsService.DEFAULT_ZONE).getYear();
        for (Month m : Month.values()) {
            if (lower.contains(m.name().toLowerCase(Locale.ROOT))) {
                found.add(YearMonth.of(currentYear, m));
            }
        }

        if (found.size() >= 2) {
            return new YearMonth[]{found.get(0), found.get(1)};
        }

        YearMonth now = YearMonth.now(AssistantAnalyticsService.DEFAULT_ZONE);
        return new YearMonth[]{now.minusMonths(1), now};
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String serializeParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
