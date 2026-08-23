package com.dailymate.assistant.domain;

import com.dailymate.assistant.domain.ParsedDomainModels.*;
import com.dailymate.assistant.tool.ToolDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Suite of domain-specific typed parsers for DailyMate AI Assistant.
 * Invariant: Pre-strips list numbering, uses strict BigDecimal numeric parsing,
 * delegates date resolution to AssistantDateResolver, and wraps extractions in ExtractionResult.
 */
@Component
public class DomainParsers {

    private final DomainPurityValidator validator;
    private final AssistantDateResolver dateResolver;

    // Bullet / List prefix cleaner: "1.", "2)", "3-", "*", "-", "•"
    private static final Pattern LIST_PREFIX_PATTERN = Pattern.compile(
            "^\\s*(?:\\d+[.)\\-:]+|[*•\\-])\\s*"
    );

    // Currency Amount Patterns
    private static final Pattern PREFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|rupees?|inr)\\s*([0-9,]+(?:\\.\\d{1,2})?)"
    );
    private static final Pattern SUFFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)([0-9,]+(?:\\.\\d{1,2})?)\\s*(?:rupees?|rs\\.?|₹|inr|bucks)"
    );
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?i)\\b(?:amount(?:\\s+is|:|=)?|cost(?:\\s+is|:|=)?|spent|spend)\\s+(?:₹|rs\\.?|inr\\s*)?([0-9,]+(?:\\.\\d{1,2})?)"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?91[- ]?)?([6-9]\\d{9})"
    );
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?i)(?:\\bat\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)"
    );
    private static final Pattern DOSAGE_PATTERN = Pattern.compile(
            "(?i)(\\d+\\s*(?:mg|ml|tablets?|units?|drops?|capsules?))"
    );
    private static final Pattern BLOOD_GROUP_PATTERN = Pattern.compile(
            "(?i)\\b(A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-)\\b"
    );
    private static final Pattern DATE_EXPRESSION_PATTERN = Pattern.compile(
            "(?i)\\b(today|yesterday|tomorrow|day before yesterday|day after tomorrow|(?:on\\s+)?(?:\\d{1,2}(?:st|nd|rd|th)?\\s+)?(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)(?:\\s+\\d{1,2}(?:st|nd|rd|th)?)?(?:\\s*,?\\s*\\d{4})?|(?:on\\s+)?\\d{4}-\\d{2}-\\d{2}|(?:on\\s+)?\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}|(?:on\\s+)?(?:last|this|next|coming)?\\s*(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday))\\b"
    );

    public DomainParsers(DomainPurityValidator validator, AssistantDateResolver dateResolver) {
        this.validator = validator;
        this.dateResolver = dateResolver;
    }

    public int countSuppliedItems(String payload) {
        if (payload == null || payload.isBlank()) return 0;

        Matcher numberedMatcher = Pattern.compile("(?m)^\\s*\\d+[.)\\-:]+\\s+").matcher(payload);
        int numberedCount = 0;
        while (numberedMatcher.find()) {
            numberedCount++;
        }
        if (numberedCount > 0) return numberedCount;

        Matcher bulletMatcher = Pattern.compile("(?m)^\\s*[*•\\-]\\s+").matcher(payload);
        int bulletCount = 0;
        while (bulletMatcher.find()) {
            bulletCount++;
        }
        if (bulletCount > 0) return bulletCount;

        String[] lines = splitPayload(payload);
        int count = 0;
        for (String l : lines) {
            if (!l.trim().isBlank()) count++;
        }
        return count;
    }

    private String[] splitPayload(String payload) {
        if (payload == null || payload.isBlank()) return new String[0];

        // 1. If payload contains newlines or semicolons, split strictly on newline/semicolon boundaries
        if (payload.contains("\n") || payload.contains(";")) {
            return payload.split("[;\\n]+");
        }

        // 2. If payload is an inline numbered list: "1. Expense A 2. Expense B"
        if (Pattern.compile("(?<=\\S)\\s+(?=\\d+[.)\\-:]+\\s+)").matcher(payload).find()) {
            return payload.split("(?<=\\S)\\s+(?=\\d+[.)\\-:]+\\s+)");
        }

        // 3. For comma-separated lists, split on commas EXCEPT:
        //    - Commas within date expressions like "August 1, 2026" or "Aug 10, 2026"
        //    - Commas within numbers like "1,850" or "2,450"
        return payload.split("(?<!\\b(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\s+\\d{1,2})\\s*,\\s*(?!\\d{4}\\b|\\d{3}(?:\\b|\\D))");
    }

    public ExtractionResult<ParsedExpense> parseExpensesResult(String payload) {
        if (payload == null || payload.isBlank()) {
            return ExtractionResult.failure(ToolDomain.EXPENSE, "BULK_CREATE", List.of("No valid expenses could be extracted."));
        }
        int expectedCount = countSuppliedItems(payload);
        List<ParsedExpense> items = parseExpenses(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.EXPENSE, "BULK_CREATE", List.of("No valid expenses could be extracted."));
        }
        if (expectedCount > 0 && items.size() != expectedCount) {
            return ExtractionResult.failure(ToolDomain.EXPENSE, "BULK_CREATE", List.of(
                    "Expected " + expectedCount + " items from input list, but parsed " + items.size() + " valid items. Please check the format."
            ));
        }
        return ExtractionResult.success(ToolDomain.EXPENSE, "BULK_CREATE", items);
    }

    public List<ParsedExpense> parseExpenses(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedExpense> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            // Step 1: Strip leading list prefixes ("1. ", "2) ", "* ", "- ")
            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            // Step 2: Skip if line contains a 10-digit phone number (likely contact or provider)
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanedLine);
            if (phoneMatcher.find()) {
                continue;
            }

            // Step 3: Extract and resolve date FIRST, and mask date expressions from amount scanning
            LocalDate spentOn = dateResolver.getToday();
            String dateExpression = null;
            Matcher dateMatcher = DATE_EXPRESSION_PATTERN.matcher(cleanedLine);
            if (dateMatcher.find()) {
                dateExpression = dateMatcher.group(1);
                spentOn = dateResolver.resolveDate(dateExpression).orElse(dateResolver.getToday());
            }

            String lineWithoutDate = cleanedLine;
            if (dateExpression != null) {
                lineWithoutDate = lineWithoutDate.replace(dateExpression, " ");
            }

            // Also mask any standalone ISO or DMY dates or 4-digit years in date patterns
            lineWithoutDate = lineWithoutDate.replaceAll("\\b(?:19|20)\\d{2}-\\d{2}-\\d{2}\\b", " ")
                    .replaceAll("\\b\\d{1,2}[/.-]\\d{1,2}[/.-](?:19|20)\\d{2}\\b", " ");

            // Step 4: Extract Amount with strict currency and numeric parsing
            BigDecimal amount = null;
            String amountSourceText = null;

            Matcher prefAmt = PREFIX_AMOUNT_PATTERN.matcher(lineWithoutDate);
            if (prefAmt.find()) {
                amount = parseAmountString(prefAmt.group(1));
                amountSourceText = prefAmt.group(0);
            } else {
                Matcher suffAmt = SUFFIX_AMOUNT_PATTERN.matcher(lineWithoutDate);
                if (suffAmt.find()) {
                    amount = parseAmountString(suffAmt.group(1));
                    amountSourceText = suffAmt.group(0);
                } else {
                    Matcher genAmt = GENERIC_AMOUNT_PATTERN.matcher(lineWithoutDate);
                    if (genAmt.find()) {
                        amount = parseAmountString(genAmt.group(1));
                        amountSourceText = genAmt.group(0);
                    }
                }
            }

            if (amount == null) {
                // If there's a standalone number, ensure it is NOT a 4-digit year (1900-2099)
                Matcher digitMatcher = Pattern.compile("(?i)\\b(\\d+(?:\\.\\d{1,2})?)\\b").matcher(lineWithoutDate);
                while (digitMatcher.find()) {
                    String candidate = digitMatcher.group(1);
                    if (candidate.matches("^(?:19|20)\\d{2}$")) {
                        continue; // Discard 4-digit years
                    }
                    amount = parseAmountString(candidate);
                    amountSourceText = digitMatcher.group(0);
                    break;
                }
            }

            if (amount == null) continue;

            // Step 5: Clean title/description
            String desc = lineWithoutDate;
            if (amountSourceText != null) {
                desc = desc.replace(amountSourceText, " ");
            }
            desc = desc.replaceAll("(?i)\\b(?:amount|spent|spend|cost|on|for|is|=|:)\\b", " ");
            desc = cleanText(desc);

            // ANTI-FABRICATION INVARIANT:
            // An expense record may only be generated from a source line containing
            // a valid expense description + validated monetary amount.
            // Never generate a generic "Expense" record from an isolated number or date fragment.
            if (desc.isBlank() || desc.length() < 2 || desc.matches("^[0-9\\s—\\-.:;,]+$") || isGenericWordOnly(desc)) {
                continue;
            }

            String cat = inferExpenseCategory(desc, cleanedLine);
            ParsedExpense exp = new ParsedExpense(
                    capitalize(desc),
                    amount,
                    cat,
                    spentOn,
                    "Imported via Assistant",
                    cleanedLine
            );

            if (validator.isValidExpense(exp)) {
                results.add(exp);
            }
        }
        return results;
    }

    public ExtractionResult<ParsedMedicineReminder> parseMedicineRemindersResult(String payload) {
        List<ParsedMedicineReminder> items = parseMedicineReminders(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.MEDICINE, "BULK_CREATE", List.of("No valid medicine reminders could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.MEDICINE, "BULK_CREATE", items);
    }

    public List<ParsedMedicineReminder> parseMedicineReminders(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedMedicineReminder> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            // Extract Dosage
            String dosage = "1 dose";
            Matcher doseMatcher = DOSAGE_PATTERN.matcher(cleanedLine);
            if (doseMatcher.find()) {
                dosage = doseMatcher.group(1);
            }

            // Extract Time
            LocalTime remindAt = LocalTime.of(9, 0); // default 9:00 AM
            Matcher timeMatcher = Pattern.compile("(?i)(?:\\bat\\s+)?(\\d{1,2}):(\\d{2})(?:\\s*(am|pm))?|(?i)(?:\\bat\\s+)?(\\d{1,2})(?:\\s*(am|pm))").matcher(cleanedLine);
            if (timeMatcher.find()) {
                if (timeMatcher.group(1) != null) {
                    int hour = Integer.parseInt(timeMatcher.group(1));
                    int min = Integer.parseInt(timeMatcher.group(2));
                    String meridiem = timeMatcher.group(3);
                    if (meridiem != null && meridiem.equalsIgnoreCase("pm") && hour < 12) {
                        hour += 12;
                    } else if (meridiem != null && meridiem.equalsIgnoreCase("am") && hour == 12) {
                        hour = 0;
                    }
                    remindAt = LocalTime.of(hour, min);
                } else if (timeMatcher.group(4) != null) {
                    int hour = Integer.parseInt(timeMatcher.group(4));
                    String meridiem = timeMatcher.group(5);
                    if (meridiem != null && meridiem.equalsIgnoreCase("pm") && hour < 12) {
                        hour += 12;
                    } else if (meridiem != null && meridiem.equalsIgnoreCase("am") && hour == 12) {
                        hour = 0;
                    }
                    remindAt = LocalTime.of(hour, 0);
                }
            }

            // Extract Medicine Name
            String name = cleanedLine
                    .replaceAll("(?i)\\d+\\s*(?:mg|ml|tablets?|units?|drops?|capsules?)", "")
                    .replaceAll("(?i)(?:\\bat\\s+)?\\d{1,2}:\\d{2}(?:\\s*(?:am|pm))?", "")
                    .replaceAll("(?i)(?:\\bat\\s+)?\\d{1,2}\\s*(?:am|pm)", "")
                    .replaceAll("(?i)\\bat\\s+\\d{1,2}\\b", "")
                    .replaceAll("(?i)^(?:add|set|create|schedule)?\\s*(?:a\\s+)?(?:medicine\\s+)?reminders?\\s*(?:for|to take)?\\s*", "")
                    .replaceAll("(?i)^(?:take|remind|schedule|add|medicine)?\\s*", "")
                    .trim();
            name = cleanText(name);

            if (!name.isBlank()) {
                ParsedMedicineReminder rem = new ParsedMedicineReminder(
                        capitalize(name),
                        dosage,
                        "DAILY",
                        remindAt,
                        "Created via Assistant",
                        cleanedLine
                );
                if (validator.isValidMedicineReminder(rem)) {
                    results.add(rem);
                }
            }
        }
        return results;
    }

    public ExtractionResult<ParsedEmergencyContact> parseEmergencyContactsResult(String payload) {
        List<ParsedEmergencyContact> items = parseEmergencyContacts(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.EMERGENCY, "BULK_CREATE", List.of("No valid emergency contacts could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.EMERGENCY, "BULK_CREATE", items);
    }

    public List<ParsedEmergencyContact> parseEmergencyContacts(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedEmergencyContact> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String phone = null;
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanedLine);
            if (phoneMatcher.find()) {
                phone = phoneMatcher.group(1);
            }

            String relationship = "Family";
            Matcher relMatcher = Pattern.compile("(?i)(father|mother|brother|sister|wife|husband|son|daughter|friend|doctor|colleague|cousin)").matcher(cleanedLine);
            if (relMatcher.find()) {
                relationship = capitalize(relMatcher.group(1));
            }

            String name = cleanedLine
                    .replaceAll("(?:\\+?91[- ]?)?[6-9]\\d{9}", "")
                    .replaceAll("(?i)^(?:add|contact|ice|phone|named|called)?\\s*", "")
                    .trim();
            name = cleanText(name);
            if (name.isBlank() || name.equalsIgnoreCase("my") || name.equalsIgnoreCase("contact")) {
                name = relationship;
            }

            if (phone != null) {
                ParsedEmergencyContact contact = new ParsedEmergencyContact(
                        capitalize(name),
                        relationship,
                        phone,
                        "FAMILY",
                        "ICE Contact via Assistant",
                        cleanedLine
                );
                if (validator.isValidEmergencyContact(contact)) {
                    results.add(contact);
                }
            }
        }
        return results;
    }

    public ExtractionResult<ParsedBloodRequest> parseBloodRequestsResult(String payload) {
        List<ParsedBloodRequest> items = parseBloodRequests(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.BLOOD, "BULK_CREATE", List.of("No valid blood donation requests could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.BLOOD, "BULK_CREATE", items);
    }

    public List<ParsedBloodRequest> parseBloodRequests(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedBloodRequest> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String bloodGroup = null;
            Matcher bgMatcher = BLOOD_GROUP_PATTERN.matcher(cleanedLine);
            if (bgMatcher.find()) {
                bloodGroup = bgMatcher.group(1).toUpperCase(Locale.ROOT);
            }

            String phone = "9876543210";
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanedLine);
            if (phoneMatcher.find()) {
                phone = phoneMatcher.group(1);
            }

            String hospital = "City Hospital";
            Matcher hospMatcher = Pattern.compile("(?i)(?:at|in|hospital)\\s+([a-zA-Z0-9\\s]+hospital|[a-zA-Z0-9\\s]+clinic)").matcher(cleanedLine);
            if (hospMatcher.find()) {
                hospital = capitalize(cleanText(hospMatcher.group(1)));
            }

            String name = cleanedLine
                    .replaceAll("(?i)\\b(?:A|B|AB|O)[+-]\\b", "")
                    .replaceAll("(?:\\+?91[- ]?)?[6-9]\\d{9}", "")
                    .replaceAll("(?i)\\b(?:urgent|blood|request|needed|require|patient|for|at)\\b", " ")
                    .trim();
            name = cleanText(name);
            if (name.isBlank()) name = "Patient";

            if (bloodGroup != null) {
                ParsedBloodRequest req = new ParsedBloodRequest(
                        capitalize(name),
                        bloodGroup,
                        hospital,
                        phone,
                        "HIGH",
                        "Blood request via Assistant",
                        cleanedLine
                );
                results.add(req);
            }
        }
        return results;
    }

    public ExtractionResult<ParsedLostItem> parseLostItemsResult(String payload) {
        List<ParsedLostItem> items = parseLostItems(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.LOST_FOUND, "BULK_CREATE", List.of("No valid lost & found items could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.LOST_FOUND, "BULK_CREATE", items);
    }

    public List<ParsedLostItem> parseLostItems(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedLostItem> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String location = "Local Area";
            Matcher locMatcher = Pattern.compile("(?i)(?:near|at|in)\\s+([a-zA-Z0-9\\s]+)").matcher(cleanedLine);
            if (locMatcher.find()) {
                location = capitalize(cleanText(locMatcher.group(1)));
            }

            String type = cleanedLine.toLowerCase(Locale.ROOT).contains("found") ? "FOUND" : "LOST";
            String title = capitalize(cleanText(cleanedLine));

            ParsedLostItem item = new ParsedLostItem(
                    title,
                    cleanedLine,
                    location,
                    type,
                    cleanedLine
            );
            if (validator.isValidLostItem(item)) {
                results.add(item);
            }
        }
        return results;
    }

    public ExtractionResult<ParsedComplaint> parseComplaintsResult(String payload) {
        List<ParsedComplaint> items = parseComplaints(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.COMPLAINT, "BULK_CREATE", List.of("No valid complaints could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.COMPLAINT, "BULK_CREATE", items);
    }

    public List<ParsedComplaint> parseComplaints(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedComplaint> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String location = "Community Area";
            Matcher locMatcher = Pattern.compile("(?i)(?:near|at|in)\\s+([a-zA-Z0-9\\s]+)").matcher(cleanedLine);
            if (locMatcher.find()) {
                location = capitalize(cleanText(locMatcher.group(1)));
            }

            String title = capitalize(cleanText(cleanedLine));
            ParsedComplaint comp = new ParsedComplaint(
                    title,
                    cleanedLine,
                    "Infrastructure",
                    location,
                    cleanedLine
            );
            if (validator.isValidComplaint(comp)) {
                results.add(comp);
            }
        }
        return results;
    }

    public ExtractionResult<ParsedEvent> parseEventsResult(String payload) {
        List<ParsedEvent> items = parseEvents(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.EVENTS, "BULK_CREATE", List.of("No valid events could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.EVENTS, "BULK_CREATE", items);
    }

    public List<ParsedEvent> parseEvents(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedEvent> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            LocalDate eventDate = dateResolver.getToday().plusDays(7);
            Matcher dateMatcher = DATE_EXPRESSION_PATTERN.matcher(cleanedLine);
            if (dateMatcher.find()) {
                eventDate = dateResolver.resolveDate(dateMatcher.group(1)).orElse(eventDate);
            }

            LocalTime eventTime = LocalTime.of(10, 0);
            Matcher timeMatcher = TIME_PATTERN.matcher(cleanedLine);
            if (timeMatcher.find()) {
                int hour = Integer.parseInt(timeMatcher.group(1));
                int min = timeMatcher.group(2) != null ? Integer.parseInt(timeMatcher.group(2)) : 0;
                String meridiem = timeMatcher.group(3);
                if (meridiem != null && meridiem.equalsIgnoreCase("pm") && hour < 12) {
                    hour += 12;
                } else if (meridiem != null && meridiem.equalsIgnoreCase("am") && hour == 12) {
                    hour = 0;
                }
                eventTime = LocalTime.of(hour, min);
            }

            String title = cleanedLine
                    .replaceAll("(?i)(?:on\\s+)?\\d{1,2}\\s+[a-zA-Z]+", "")
                    .replaceAll("(?i)(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)", "")
                    .trim();
            title = cleanText(title);

            if (!title.isBlank()) {
                ParsedEvent ev = new ParsedEvent(
                        capitalize(title),
                        cleanedLine,
                        eventDate,
                        eventTime,
                        "Community Center",
                        "General",
                        cleanedLine
                );
                if (validator.isValidEvent(ev)) {
                    results.add(ev);
                }
            }
        }
        return results;
    }

    public ExtractionResult<ParsedGroceryItem> parseGroceryItemsResult(String payload) {
        List<ParsedGroceryItem> items = parseGroceryItems(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.GROCERY, "BULK_CREATE", List.of("No valid grocery items could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.GROCERY, "BULK_CREATE", items);
    }

    public List<ParsedGroceryItem> parseGroceryItems(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedGroceryItem> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            BigDecimal price = BigDecimal.valueOf(50);
            Matcher amtMatcher = PREFIX_AMOUNT_PATTERN.matcher(cleanedLine);
            if (amtMatcher.find()) {
                price = parseAmountString(amtMatcher.group(1));
            } else {
                Matcher suffAmt = SUFFIX_AMOUNT_PATTERN.matcher(cleanedLine);
                if (suffAmt.find()) {
                    price = parseAmountString(suffAmt.group(1));
                }
            }

            String store = "Local Market";
            Matcher storeMatcher = Pattern.compile("(?i)(?:at|from)\\s+([a-zA-Z0-9\\s]+)").matcher(cleanedLine);
            if (storeMatcher.find()) {
                store = capitalize(cleanText(storeMatcher.group(1)));
            }

            String name = cleanedLine
                    .replaceAll("(?i)(?:₹|rs\\.?|rupees?|inr)\\s*[0-9,]+(?:\\.\\d{1,2})?", "")
                    .replaceAll("(?i)[0-9,]+(?:\\.\\d{1,2})?\\s*(?:rupees?|rs\\.?|₹|inr|bucks)", "")
                    .replaceAll("(?i)(?:at|from)\\s+[a-zA-Z0-9\\s]+", "")
                    .trim();
            name = cleanText(name);
            if (name.isBlank()) name = "Grocery Item";

            results.add(new ParsedGroceryItem(
                    capitalize(name),
                    "General",
                    store,
                    price != null ? price : BigDecimal.valueOf(50),
                    "1 kg",
                    "Pune",
                    cleanedLine
            ));
        }
        return results;
    }

    public ExtractionResult<ParsedJobPost> parseJobPostsResult(String payload) {
        List<ParsedJobPost> items = parseJobPosts(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.JOBS, "BULK_CREATE", List.of("No valid job posts could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.JOBS, "BULK_CREATE", items);
    }

    public List<ParsedJobPost> parseJobPosts(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedJobPost> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String phone = "9876543210";
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanedLine);
            if (phoneMatcher.find()) {
                phone = phoneMatcher.group(1);
            }

            String salary = "Competitive";
            Matcher amtMatcher = PREFIX_AMOUNT_PATTERN.matcher(cleanedLine);
            if (amtMatcher.find()) {
                salary = "₹" + amtMatcher.group(1);
            }

            String title = cleanedLine
                    .replaceAll("(?:\\+?91[- ]?)?[6-9]\\d{9}", "")
                    .replaceAll("(?i)(?:₹|rs\\.?|rupees?|inr)\\s*[0-9,]+(?:\\.\\d{1,2})?", "")
                    .trim();
            title = cleanText(title);
            if (title.isBlank()) title = "Neighborhood Opening";

            results.add(new ParsedJobPost(
                    capitalize(title),
                    "Services",
                    "Pune",
                    "Full-time",
                    salary,
                    "Community Employer",
                    phone,
                    "jobs@dailymate.local",
                    cleanedLine,
                    cleanedLine
            ));
        }
        return results;
    }

    public ExtractionResult<ParsedNotification> parseNotificationsResult(String payload) {
        List<ParsedNotification> items = parseNotifications(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.NOTIFICATION, "BULK_CREATE", List.of("No valid notifications could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.NOTIFICATION, "BULK_CREATE", items);
    }

    public List<ParsedNotification> parseNotifications(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedNotification> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String title = "Community Alert";
            String message = cleanedLine;

            if (cleanedLine.contains(":") || cleanedLine.contains("—") || cleanedLine.contains(" - ")) {
                String[] parts = cleanedLine.split("[:—]|\\s+-\\s+", 2);
                if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    title = capitalize(parts[0].trim());
                    message = parts[1].trim();
                }
            }

            results.add(new ParsedNotification(
                    title,
                    message,
                    "COMMUNITY",
                    cleanedLine
            ));
        }
        return results;
    }

    public ExtractionResult<ParsedProvider> parseProvidersResult(String payload) {
        List<ParsedProvider> items = parseProviders(payload);
        if (items.isEmpty()) {
            return ExtractionResult.failure(ToolDomain.MARKETPLACE, "BULK_CREATE", List.of("No valid providers could be extracted."));
        }
        return ExtractionResult.success(ToolDomain.MARKETPLACE, "BULK_CREATE", items);
    }

    public List<ParsedProvider> parseProviders(String payload) {
        if (payload == null || payload.isBlank()) return List.of();

        List<ParsedProvider> results = new ArrayList<>();
        String[] lines = splitPayload(payload);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            String cleanedLine = LIST_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
            if (cleanedLine.isBlank()) continue;

            String phone = "9876543210";
            Matcher phoneMatcher = PHONE_PATTERN.matcher(cleanedLine);
            if (phoneMatcher.find()) {
                phone = phoneMatcher.group(1);
            }

            String category = "Plumbing";
            if (cleanedLine.toLowerCase(Locale.ROOT).contains("electric")) category = "Electrical";
            else if (cleanedLine.toLowerCase(Locale.ROOT).contains("carpenter")) category = "Carpentry";
            else if (cleanedLine.toLowerCase(Locale.ROOT).contains("tutor")) category = "Education";
            else if (cleanedLine.toLowerCase(Locale.ROOT).contains("clean")) category = "Cleaning";

            String name = cleanedLine
                    .replaceAll("(?:\\+?91[- ]?)?[6-9]\\d{9}", "")
                    .replaceAll("(?i)\\b(?:plumber|electrician|carpenter|tutor|service|provider|named|called|with|phone|contact)\\b", " ")
                    .trim();
            name = cleanText(name);
            if (name.isBlank()) name = category + " Services";

            results.add(new ParsedProvider(
                    capitalize(name),
                    category,
                    phone,
                    "service@dailymate.local",
                    "Pune, MH",
                    cleanedLine,
                    cleanedLine
            ));
        }
        return results;
    }

    private BigDecimal parseAmountString(String raw) {
        if (raw == null) return null;
        try {
            String clean = raw.replace(",", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("(?i)^(?:my|an|a|the|for|on|at|and|with|to|in)\\s+", "")
                .replaceAll("(?i)\\s+(?:and|for|to|with|at|on|in)$", "")
                .replaceAll("^[\\s,.:;\\-—–]+|[\\s,.:;\\-—–]+$", "")
                .trim();
    }

    private boolean isGenericWordOnly(String desc) {
        String clean = desc.trim().toLowerCase(Locale.ROOT);
        return clean.equals("expense") || clean.equals("expenses") || clean.equals("other")
                || clean.equals("item") || clean.equals("items") || clean.equals("cost")
                || clean.equals("amount") || clean.equals("spending");
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String inferExpenseCategory(String description, String rawText) {
        String combined = (description + " " + rawText).toLowerCase(Locale.ROOT);
        if (combined.contains("lunch") || combined.contains("dinner") || combined.contains("breakfast")
                || combined.contains("tea") || combined.contains("coffee") || combined.contains("food")
                || combined.contains("restaurant") || combined.contains("cafe") || combined.contains("groceries")
                || combined.contains("fruits") || combined.contains("vegetables") || combined.contains("khichadi")) {
            return combined.contains("groceries") ? "Groceries" : "Food & Dining";
        }
        if (combined.contains("petrol") || combined.contains("diesel") || combined.contains("fuel")
                || combined.contains("travel") || combined.contains("cab") || combined.contains("uber")
                || combined.contains("auto") || combined.contains("bus") || combined.contains("train")
                || combined.contains("transportation") || combined.contains("transport")) {
            return "Travel";
        }
        if (combined.contains("bill") || combined.contains("electric") || combined.contains("water")
                || combined.contains("wifi") || combined.contains("internet") || combined.contains("rent") || combined.contains("recharge")) {
            return "Utilities";
        }
        if (combined.contains("watch") || combined.contains("shopping") || combined.contains("clothes")
                || combined.contains("shoes") || combined.contains("electronics") || combined.contains("book")) {
            return "Shopping";
        }
        if (combined.contains("pharmacy") || combined.contains("medicine") || combined.contains("medical")
                || combined.contains("hospital") || combined.contains("doctor") || combined.contains("health")) {
            return "Healthcare";
        }
        if (combined.contains("movie") || combined.contains("cinema") || combined.contains("theatre")
                || combined.contains("entertainment") || combined.contains("game")) {
            return "Entertainment";
        }
        return "Other";
    }
}
