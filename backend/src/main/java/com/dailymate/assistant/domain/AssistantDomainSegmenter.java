package com.dailymate.assistant.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Reusable Domain Segmentation Layer for DailyMate AI Assistant.
 * Identifies explicit section headers, single-domain bulk lists, and isolates domain payloads
 * before domain parsers run.
 * Invariant: A domain parser must never receive content belonging to another domain.
 */
@Component
public class AssistantDomainSegmenter {

    private record HeaderMatch(int start, int end, AssistantDomain domain, String headerLabel) {}

    private static final List<HeaderPattern> HEADER_PATTERNS = List.of(
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(expenses?|spending|bills?)\\s*:"), AssistantDomain.EXPENSE),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(medicine\\s+reminders?|medicines?|prescriptions?)\\s*:"), AssistantDomain.MEDICINE_REMINDER),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(emergency\\s+contacts?|ice\\s+contacts?)\\s*:"), AssistantDomain.EMERGENCY_CONTACT),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(complaints?|community\\s+complaints?|civic\\s+issues?)\\s*:"), AssistantDomain.COMPLAINT),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(lost\\s+items?|lost\\s*&?\\s*found|found\\s+items?)\\s*:"), AssistantDomain.LOST_FOUND),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(events?|community\\s+events?|gatherings?)\\s*:"), AssistantDomain.EVENT),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(notifications?|alerts?)\\s*:"), AssistantDomain.NOTIFICATION),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(blood\\s+requests?|blood\\s+donation)\\s*:"), AssistantDomain.BLOOD_REQUEST),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(marketplace|service\\s+providers?|plumbers?|electricians?)\\s*:"), AssistantDomain.MARKETPLACE),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(jobs?|job\\s+openings?|vacancies)\\s*:"), AssistantDomain.JOB),
            new HeaderPattern(Pattern.compile("(?i)(?:^|(?<=[.\n]))\\s*(?:[-*•#]+\\s*)?(groceries|grocery\\s+items?|grocery\\s+prices?|price\\s+comparison)\\s*:"), AssistantDomain.GROCERY)
    );

    private static final Pattern LIST_INDICATOR_PATTERN = Pattern.compile("(?m)(?:^\\s*\\d+[.)]\\s+|[-*•]\\s+)");

    private record HeaderPattern(Pattern pattern, AssistantDomain domain) {}

    public List<DomainSegment> segment(String prompt) {
        if (prompt == null || prompt.isBlank()) return List.of();

        List<HeaderMatch> matches = new ArrayList<>();
        for (HeaderPattern hp : HEADER_PATTERNS) {
            Matcher m = hp.pattern().matcher(prompt);
            while (m.find()) {
                matches.add(new HeaderMatch(m.start(), m.end(), hp.domain(), m.group(1).trim()));
            }
        }

        if (matches.isEmpty()) {
            // Check for explicit single-domain bulk introduction phrases
            // Requirement: Must contain explicit bulk markers ("these", "following", or count >= 2, or structured list indicators)
            String lower = prompt.toLowerCase(Locale.ROOT).trim();
            boolean hasListItems = LIST_INDICATOR_PATTERN.matcher(prompt).find();
            boolean hasBulkKeyword = lower.contains("these") || lower.contains("following") || lower.contains("bulk") || lower.matches(".*\\b\\d+\\s+(?:expenses?|medicines?|reminders?|contacts?|events?|jobs?|notifications?|items?|prices?).*");

            if (hasListItems || hasBulkKeyword) {
                if (lower.contains("expense") || lower.contains("spending")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|record|import|log)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?expenses?(?:\\s+(?:to|for)\\s+(?:my\\s+)?account)?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.EXPENSE, "Expenses", payload));
                    }
                }
                if (lower.contains("medicine") || lower.contains("reminder")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|schedule|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?(?:medicine\\s+)?reminders?(?:\\s+(?:to|for)\\s+(?:my\\s+)?account)?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.MEDICINE_REMINDER, "Medicines", payload));
                    }
                }
                if (lower.contains("contact") || lower.contains("ice")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?(?:emergency\\s+|ice\\s+)?contacts?(?:\\s+(?:to|for)\\s+(?:my\\s+)?account)?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.EMERGENCY_CONTACT, "Emergency Contacts", payload));
                    }
                }
                if (lower.contains("grocery") || lower.contains("groceries")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|compare|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?grocery\\s+items?(?:\\s+(?:to|for)\\s+(?:my\\s+)?account)?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.GROCERY, "Groceries", payload));
                    }
                }
                if (lower.contains("blood")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|post|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?blood\\s+requests?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.BLOOD_REQUEST, "Blood Requests", payload));
                    }
                }
                if (lower.contains("job")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|post|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?jobs?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.JOB, "Jobs", payload));
                    }
                }
                if (lower.contains("lost") || lower.contains("found")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|post|report|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?(?:lost\\s*&?\\s*found|lost|found)\\s+items?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.LOST_FOUND, "Lost & Found", payload));
                    }
                }
                if (lower.contains("complaint")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|submit|report|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?complaints?(?:\\s+in\\s+bulk|\\s+in\\s+a\\s+batch)?(?:\\s*\\.)?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.COMPLAINT, "Complaints", payload));
                    }
                }
                if (lower.contains("event")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|post|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?events?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.EVENT, "Events", payload));
                    }
                }
                if (lower.contains("provider") || lower.contains("marketplace") || lower.contains("electrician") || lower.contains("plumber")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|register|import)\\s+(?:these|the following|all)?\\s*(?:\\d+\\s+)?providers?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.MARKETPLACE, "Marketplace", payload));
                    }
                }
                if (lower.contains("notification") || lower.contains("alert")) {
                    String payload = prompt.replaceFirst("(?i)^.*?(?:(?:add|create|import)\\s+(?:these|the following|all)\\s*(?:\\d+\\s+)?notifications?:?)\\s*", "").trim();
                    if (!payload.isBlank() && !payload.equalsIgnoreCase(prompt.trim())) {
                        return List.of(new DomainSegment(AssistantDomain.NOTIFICATION, "Notifications", payload));
                    }
                }
            }
            return List.of();
        }

        // Sort matches by their starting offset
        matches.sort(Comparator.comparingInt(HeaderMatch::start));

        List<DomainSegment> segments = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            HeaderMatch current = matches.get(i);
            int contentStart = current.end();
            int contentEnd = (i + 1 < matches.size()) ? matches.get(i + 1).start() : prompt.length();

            String payload = prompt.substring(contentStart, contentEnd).trim();
            if (payload.endsWith(".")) {
                payload = payload.substring(0, payload.length() - 1).trim();
            }

            segments.add(new DomainSegment(current.domain(), current.headerLabel(), payload));
        }

        return segments;
    }
}
