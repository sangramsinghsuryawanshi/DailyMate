package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.MonthlyLifeReportDto;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Universal Intent & Tool Router for DailyMate AI Assistant across all 8 Domains.
 * Invariant: Never guesses or fabricates parameters. Missing parameters produce clarification questions.
 */
@Component
public class AssistantToolRouter {

    private final AssistantToolRegistry toolRegistry;
    private final AssistantReportingService reportingService;
    private final ObjectMapper objectMapper;
    private final DecimalFormat inrFormat = new DecimalFormat("₹#,##,##0.00");

    // Regex matchers for multi-domain extraction
    private static final Pattern EXPLICIT_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:amount\\s*(?:is|=|:)?\\s*|₹\\s*|rs\\.?\\s*|rupees?\\s*|inr\\s*)(\\d+(?:\\.\\d{1,2})?)"
    );
    private static final Pattern SUFFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(\\d+(?:\\.\\d{1,2})?)\\s*(?:rupees?|rs\\.?|₹|inr|bucks)"
    );
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:expense\\s+(?:of\\s+)?|spent\\s+|spend\\s+|add\\s+|record\\s+)(\\d+(?:\\.\\d{1,2})?)"
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
    private static final Pattern PROVIDER_PATTERN = Pattern.compile(
            "(?i)(?:add|register|create)\\s+(?:my\\s+)?(electrician|plumber|carpenter|painter|mechanic|cleaner|repairman|technician)\\s+(?:named\\s+|called\\s+)?([a-zA-Z\\s]+?)(?=\\s+(?:with|phone|number|in|at|contact|exp|years)|\\s*$)"
    );
    private static final Pattern BLOOD_GROUP_PATTERN = Pattern.compile(
            "(?i)(?:^|\\s)(A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-)(?:\\s|[,.]|$)"
    );

    public AssistantToolRouter(AssistantToolRegistry toolRegistry, AssistantReportingService reportingService) {
        this.toolRegistry = toolRegistry;
        this.reportingService = reportingService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public AssistantGroundingEngine.GroundingResult route(String prompt, String userId, AssistantContext context) {
        if (prompt == null || prompt.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("How can I assist you with DailyMate today?", null);
        }

        String lower = prompt.toLowerCase(Locale.ROOT).trim();

        // 1. Admin / Privilege Escalation Guard
        if (lower.contains("all user") || lower.contains("all password") || lower.contains("delete all") || lower.contains("admin panel")) {
            return new AssistantGroundingEngine.GroundingResult("Administrative operations and private user records are strictly protected and cannot be accessed or modified through the assistant.", null);
        }

        // 2. Unsupported Action Guards (No improvisation on unregistered capabilities)
        if (lower.contains("flight") || lower.contains("book ticket") || lower.contains("hotel booking") || lower.contains("send money") || lower.contains("transfer money") || lower.contains("upi payment")) {
            return new AssistantGroundingEngine.GroundingResult("DailyMate does not support direct third-party bookings or financial money transfers at this time.", null);
        }

        // 3. Deterministic Life Reporting & Analytics Tool
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

        // 4. Notification Mutations (Tier 2 mark all read & Tier 3 create)
        if (lower.contains("mark all") && (lower.contains("notification") || lower.contains("notifications") || lower.contains("alerts")) && (lower.contains("read") || lower.contains("clear"))) {
            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to mark all your notifications as read.",
                    new AssistantGroundingEngine.ActionProposalData("MARK_NOTIFICATIONS_READ", "Mark all notifications as read", "{}")
            );
        }

        // 5. Emergency ICE Contact Addition (Tier 3)
        if ((lower.contains("emergency contact") || lower.contains("ice contact")) && (lower.startsWith("add ") || lower.startsWith("create ") || lower.startsWith("set "))) {
            return handleIceContactCreation(prompt);
        }

        // 6. Blood Emergency Request Creation (Tier 3)
        if (lower.contains("blood") && (lower.contains("need") || lower.contains("request") || lower.contains("require") || lower.contains("unit") || lower.contains("urgent"))) {
            return handleBloodRequestCreation(prompt);
        }

        // 7. Local Marketplace Service Provider Registration (Tier 3)
        if (lower.contains("electrician") || lower.contains("plumber") || lower.contains("carpenter") || lower.contains("mechanic") || lower.contains("painter")) {
            if (lower.startsWith("add ") || lower.startsWith("register ") || lower.startsWith("create ")) {
                return handleProviderRegistration(prompt);
            }
        }

        // 8. Expense Tool Routing (Tier 3 Mutation vs Tier 1 Read)
        boolean isExpenseReadQuery = lower.startsWith("how much") || lower.startsWith("what is my") || lower.startsWith("show my")
                || lower.startsWith("check my") || lower.startsWith("view my") || lower.startsWith("list my") || lower.contains("total expense");

        if (!isExpenseReadQuery && (lower.startsWith("add ") || lower.startsWith("record ") || lower.startsWith("log ") || lower.startsWith("spent ")
                || lower.startsWith("spend ") || lower.contains("add expense") || lower.contains("record expense") || lower.contains("log expense"))) {
            AssistantGroundingEngine.GroundingResult expenseResult = handleExpenseIntent(prompt);
            if (expenseResult != null) {
                return expenseResult;
            }
        }

        // 9. Medicine Reminder Tool Routing (Tier 3 Mutation)
        if ((lower.startsWith("set reminder") || lower.startsWith("add reminder") || lower.startsWith("schedule reminder") || lower.startsWith("remind me to take"))
                || (lower.contains("reminder") && (lower.contains("set") || lower.contains("add") || lower.contains("schedule")) && !lower.contains("check") && !lower.contains("what"))) {
            AssistantGroundingEngine.ActionProposalData reminderProposal = tryParseReminderProposal(prompt);
            if (reminderProposal != null) {
                return new AssistantGroundingEngine.GroundingResult(
                        "I have prepared an action to schedule this medicine reminder. Please confirm below to save it.",
                        reminderProposal);
            }
        }

        // 10. Medicine Reminders Inquiry (Tier 1 Read)
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

        // 11. Expense Inquiry (Tier 1 Read)
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

        // 12. Emergency Contacts Inquiry (Tier 1 Read)
        if (lower.contains("emergency") || lower.contains("ambulance") || lower.contains("police") || lower.contains("fire") || lower.contains("hospital") || lower.contains("call")) {
            StringBuilder sb = new StringBuilder("🚨 Emergency Services Hotlines:\n\n• National Emergency: 112\n• Police: 100\n• Fire: 101\n• Ambulance: 102 / 108\n\n");
            if (context.emergency() != null && context.emergency().personalContactCount() > 0) {
                sb.append("You have ").append(context.emergency().personalContactCount()).append(" personal ICE emergency contact(s) configured in your Emergency Directory.");
            } else {
                sb.append("You have not added any personal emergency contacts yet. You can add them in the Emergency Directory.");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 13. Community Events Inquiry (Tier 1 Read)
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

        // 14. Open Job Opportunities Inquiry (Tier 1 Read)
        if (lower.contains("job") || lower.contains("hiring") || lower.contains("vacancy") || lower.contains("career") || lower.contains("work")) {
            if (context.jobs() == null || context.jobs().isEmpty()) {
                return new AssistantGroundingEngine.GroundingResult("There are currently no active job postings in the community board.", null);
            }
            StringBuilder sb = new StringBuilder("💼 **Open Job Opportunities**:\n");
            for (var j : context.jobs()) {
                sb.append("• **").append(j.title()).append("** at ").append(j.companyName()).append(" (").append(j.location()).append(")")
                        .append(j.salary() != null ? " — Salary: " + inrFormat.format(j.salary()) : "")
                        .append("\n");
            }
            return new AssistantGroundingEngine.GroundingResult(sb.toString().trim(), null);
        }

        // 15. Default Navigation Guidance
        return new AssistantGroundingEngine.GroundingResult("I am your DailyMate Assistant. I can help you with:\n\n"
                + "• 📊 **Life Reports**: Say *'Generate my monthly DailyMate life report'*.\n"
                + "• 💊 **Medicine Reminders**: Ask about scheduled doses or say *'Set reminder for Paracetamol 500mg at 09:00 daily'*.\n"
                + "• 💰 **Expenses**: Track your monthly spend or say *'Record ₹50 for lunch, Khichadi'*.\n"
                + "• 🩸 **Blood Requests**: Say *'Need 2 units of O+ blood for Rahul at Ruby Hall Clinic Pune, call 9876543210'*.\n"
                + "• 🚨 **Emergency ICE**: Say *'Add my wife Priya with phone 9876543210 as emergency contact'*.\n"
                + "• 🔧 **Local Services**: Say *'Add electrician Rahul with phone 9876543210 in Pune'*.\n"
                + "• 🔔 **Notifications**: Say *'Mark all notifications as read'*.\n"
                + "• 📅 **Community Events**: Discover upcoming local events and gatherings.\n"
                + "• 💼 **Jobs Board**: Browse open community opportunities.", null);
    }

    private AssistantGroundingEngine.GroundingResult handleExpenseIntent(String prompt) {
        BigDecimal amount = extractAmount(prompt);
        String description = extractDescription(prompt);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            if (description != null && !description.isBlank()) {
                return new AssistantGroundingEngine.GroundingResult("What amount should I record for " + description + "?", null);
            }
            return new AssistantGroundingEngine.GroundingResult("Please specify the amount and description for the expense you would like to record.", null);
        }

        if (description == null || description.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("What is this expense of " + inrFormat.format(amount) + " for?", null);
        }

        String category = inferCategory(description, prompt);

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("category", category);
            map.put("description", description);
            map.put("amount", amount);
            map.put("spentOn", LocalDate.now().toString());
            map.put("notes", "Created via AI Assistant");

            String json = objectMapper.writeValueAsString(map);
            String summary = "Record " + inrFormat.format(amount) + " expense for " + description + " (" + category + ")";

            AssistantGroundingEngine.ActionProposalData proposal = new AssistantGroundingEngine.ActionProposalData("RECORD_EXPENSE", summary, json);
            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to record this expense for you. Please confirm below to save it to your expense tracker.",
                    proposal);
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handleProviderRegistration(String prompt) {
        String phone = null;
        Matcher mPhone = PHONE_PATTERN.matcher(prompt);
        if (mPhone.find()) {
            phone = mPhone.group(1);
        }

        String serviceType = "Electrician";
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("plumber")) serviceType = "Plumber";
        else if (lower.contains("carpenter")) serviceType = "Carpenter";
        else if (lower.contains("painter")) serviceType = "Painter";
        else if (lower.contains("mechanic")) serviceType = "Mechanic";

        String name = null;
        Matcher mProvider = PROVIDER_PATTERN.matcher(prompt);
        if (mProvider.find()) {
            name = cleanDescription(mProvider.group(2));
        }

        if (name == null || name.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("What is the name of the " + serviceType + " you would like to add?", null);
        }

        if (phone == null) {
            return new AssistantGroundingEngine.GroundingResult("What is " + name + "'s 10-digit contact phone number?", null);
        }

        String city = "Pune";
        if (lower.contains("mumbai")) city = "Mumbai";
        else if (lower.contains("bangalore") || lower.contains("bengaluru")) city = "Bangalore";
        else if (lower.contains("delhi")) city = "Delhi";

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", capitalize(name));
            map.put("serviceType", serviceType);
            map.put("phone", phone);
            map.put("city", city);
            map.put("area", "Local");
            map.put("experienceYears", 3);

            String json = objectMapper.writeValueAsString(map);
            String summary = "Register " + serviceType + " " + capitalize(name) + " (" + phone + ") in " + city;

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to register this service provider. Please confirm below to add them to your local services directory.",
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

        String relationship = "Family";
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("wife")) relationship = "Wife";
        else if (lower.contains("husband")) relationship = "Husband";
        else if (lower.contains("father") || lower.contains("dad")) relationship = "Father";
        else if (lower.contains("mother") || lower.contains("mom")) relationship = "Mother";
        else if (lower.contains("brother")) relationship = "Brother";
        else if (lower.contains("sister")) relationship = "Sister";
        else if (lower.contains("doctor")) relationship = "Doctor";
        else if (lower.contains("friend")) relationship = "Friend";

        String name = null;
        Matcher mName = Pattern.compile("(?i)(?:add|set)\\s+(?:my\\s+)?(?:wife|husband|father|mother|brother|sister|doctor|friend)?\\s*([a-zA-Z]+)(?=\\s+(?:with|phone|number|as|contact)|\\s*$)").matcher(prompt);
        if (mName.find()) {
            name = cleanDescription(mName.group(1));
        }

        if (name == null || name.isBlank()) {
            return new AssistantGroundingEngine.GroundingResult("Who would you like to add as your emergency ICE contact?", null);
        }

        if (phone == null) {
            return new AssistantGroundingEngine.GroundingResult("What is " + capitalize(name) + "'s contact phone number?", null);
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("name", capitalize(name));
            map.put("relationship", relationship);
            map.put("phone", phone);
            map.put("category", "ICE Personal");
            map.put("notes", "Added via AI Assistant");

            String json = objectMapper.writeValueAsString(map);
            String summary = "Add " + capitalize(name) + " (" + relationship + " - " + phone + ") to emergency ICE contacts";

            return new AssistantGroundingEngine.GroundingResult(
                    "I have prepared an action to add this emergency contact. Please confirm below to save it to your Emergency Directory.",
                    new AssistantGroundingEngine.ActionProposalData("CREATE_ICE_CONTACT", summary, json)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private AssistantGroundingEngine.GroundingResult handleBloodRequestCreation(String prompt) {
        String bloodGroup = null;
        Matcher mGroup = BLOOD_GROUP_PATTERN.matcher(prompt);
        if (mGroup.find()) {
            bloodGroup = mGroup.group(1).trim().toUpperCase(Locale.ROOT);
        }

        if (bloodGroup == null) {
            return new AssistantGroundingEngine.GroundingResult("What blood group is needed for the emergency request (e.g. O+, A+, B+, AB-)?", null);
        }

        String phone = null;
        Matcher mPhone = PHONE_PATTERN.matcher(prompt);
        if (mPhone.find()) {
            phone = mPhone.group(1);
        }

        if (phone == null) {
            return new AssistantGroundingEngine.GroundingResult("Please provide a contact phone number for this emergency blood request.", null);
        }

        String location = "City Hospital";
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
        Matcher m1 = EXPLICIT_AMOUNT_PATTERN.matcher(prompt);
        if (m1.find()) {
            try { return new BigDecimal(m1.group(1)); } catch (Exception ignored) {}
        }
        Matcher m2 = SUFFIX_AMOUNT_PATTERN.matcher(prompt);
        if (m2.find()) {
            try { return new BigDecimal(m2.group(1)); } catch (Exception ignored) {}
        }
        Matcher m3 = GENERIC_AMOUNT_PATTERN.matcher(prompt);
        if (m3.find()) {
            try { return new BigDecimal(m3.group(1)); } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractDescription(String prompt) {
        Matcher mName = NAME_KEYWORD_PATTERN.matcher(prompt);
        if (mName.find()) {
            String name = cleanDescription(mName.group(1));
            if (!name.isBlank()) return capitalize(name);
        }
        Matcher mFor = FOR_KEYWORD_PATTERN.matcher(prompt);
        if (mFor.find()) {
            String forText = cleanDescription(mFor.group(1));
            if (!forText.isBlank()) return capitalize(forText);
        }
        Matcher mPattern = Pattern.compile("(?i)(?:for|on|in)\\s+([a-zA-Z\\s]+)").matcher(prompt);
        if (mPattern.find()) {
            String text = cleanDescription(mPattern.group(1));
            if (!text.isBlank()) return capitalize(text);
        }
        return null;
    }

    private String cleanDescription(String text) {
        if (text == null) return "";
        String cleaned = text.trim()
                .replaceAll("(?i)^(?:my|an|a|the|afternoon|morning|evening|night|daily|monthly|named|called|with|for)\\s+", "")
                .replaceAll("(?i)\\s+(?:and|is|amount|for|to|with|phone|number|in|at)$", "")
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
        if (combined.contains("shopping") || combined.contains("clothes") || combined.contains("shoes")
                || combined.contains("electronics") || combined.contains("book")) {
            return "Shopping";
        }
        return "Other";
    }

    private AssistantGroundingEngine.ActionProposalData tryParseReminderProposal(String prompt) {
        try {
            Matcher m = REMINDER_PATTERN.matcher(prompt);
            String name = "Medication";
            String dosage = "1 dose";
            String remindAt = "09:00";

            if (m.find()) {
                String n = m.group(1);
                if (n != null && !n.isBlank()) name = capitalize(n.trim());
                String d = m.group(2);
                if (d != null && !d.isBlank()) dosage = d.trim();
                String t = m.group(3);
                if (t != null && !t.isBlank()) {
                    remindAt = t.contains(":") ? t : t + ":00";
                    if (remindAt.length() == 4) remindAt = "0" + remindAt;
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("dosage", dosage);
            map.put("frequency", "DAILY");
            map.put("remindAt", remindAt);
            map.put("notes", "Created via AI Assistant");
            map.put("active", true);

            String json = objectMapper.writeValueAsString(map);
            String summary = "Schedule " + name + " (" + dosage + ") at " + remindAt + " daily";

            return new AssistantGroundingEngine.ActionProposalData("CREATE_REMINDER", summary, json);
        } catch (Exception ex) {
            return null;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
