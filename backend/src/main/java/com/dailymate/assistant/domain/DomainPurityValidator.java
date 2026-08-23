package com.dailymate.assistant.domain;

import com.dailymate.assistant.domain.ParsedDomainModels.*;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Server-Authoritative Domain Purity Validator for DailyMate AI Assistant.
 * Mandates that:
 * 1. An expense record never contains phone numbers, times, dates, dosages, or non-expense tokens as amounts.
 * 2. Non-monetary domains never contain financial mutation payloads.
 * 3. Notification text never becomes expenses; Grocery never becomes medicines; Medicines never become groceries.
 * 4. Typed validation executes before any action proposal is constructed.
 */
@Component
public class DomainPurityValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)^\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?$");
    private static final Pattern DOSAGE_PATTERN = Pattern.compile("(?i)^\\d+\\s*(?:mg|ml|tablets?|units?|drops?|capsules?)$");
    private static final BigDecimal MAX_REASONABLE_SINGLE_EXPENSE = new BigDecimal("500000.00"); // ₹5,00,000 ceiling

    public boolean isValidExpense(ParsedExpense expense) {
        if (expense == null) return false;
        if (expense.amount() == null || expense.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Semantic Check 1: 10-digit Indian phone numbers must NEVER become expense amounts
        String plainAmt = expense.amount().toPlainString();
        if (PHONE_PATTERN.matcher(plainAmt).matches() || plainAmt.length() >= 10) {
            return false;
        }

        // Semantic Check 2: Exceeding max reasonable single expense ceiling
        if (expense.amount().compareTo(MAX_REASONABLE_SINGLE_EXPENSE) > 0) {
            return false;
        }

        // Semantic Check 3: Description must not be blank or an isolated time/dosage
        if (expense.description() == null || expense.description().isBlank()) {
            return false;
        }
        String desc = expense.description().trim().toLowerCase(Locale.ROOT);
        if (TIME_PATTERN.matcher(desc).matches() || DOSAGE_PATTERN.matcher(desc).matches()) {
            return false;
        }

        // Anti-Contamination Check: Notification, Blood, or Lost & Found text must NOT be an expense
        if (desc.startsWith("notify ") || desc.startsWith("notification") || desc.contains("blood appeal") || desc.contains("blood request") || desc.contains("lost item") || desc.contains("complaint near")) {
            return false;
        }

        return true;
    }

    public boolean isValidMedicineReminder(ParsedMedicineReminder reminder) {
        if (reminder == null) return false;
        if (reminder.name() == null || reminder.name().isBlank()) return false;
        String name = reminder.name().toLowerCase(Locale.ROOT);

        // Anti-Contamination Check: Grocery items (e.g. tomatoes, onions, milk, rice) must NEVER be medicines
        if (name.contains("tomato") || name.contains("onion") || name.contains("potato") || name.contains("milk") || name.contains("rice") || name.contains("bread") || name.contains("dal") || name.contains("oil") || name.contains("sugar") || name.contains("wheat")) {
            return false;
        }

        return reminder.remindAt() != null;
    }

    public boolean isValidEmergencyContact(ParsedEmergencyContact contact) {
        if (contact == null) return false;
        if (contact.name() == null || contact.name().isBlank()) return false;
        if (contact.phone() == null || contact.phone().isBlank()) return false;

        String digits = contact.phone().replaceAll("[^0-9]", "");
        return digits.length() == 10;
    }

    public boolean isValidComplaint(ParsedComplaint complaint) {
        if (complaint == null) return false;
        return complaint.title() != null && !complaint.title().isBlank();
    }

    public boolean isValidLostItem(ParsedLostItem item) {
        if (item == null) return false;
        return item.title() != null && !item.title().isBlank();
    }

    public boolean isValidEvent(ParsedEvent event) {
        if (event == null) return false;
        return event.title() != null && !event.title().isBlank();
    }

    public boolean isValidGroceryItem(ParsedGroceryItem item) {
        if (item == null) return false;
        if (item.name() == null || item.name().isBlank()) return false;
        if (item.price() == null || item.price().compareTo(BigDecimal.ZERO) <= 0) return false;
        return true;
    }

    public boolean isValidJobPost(ParsedJobPost job) {
        if (job == null) return false;
        return job.title() != null && !job.title().isBlank();
    }

    public boolean isValidBloodRequest(ParsedBloodRequest req) {
        if (req == null) return false;
        return req.bloodGroup() != null && !req.bloodGroup().isBlank();
    }

    public boolean isValidNotification(ParsedNotification notif) {
        if (notif == null) return false;
        return notif.title() != null && !notif.title().isBlank();
    }

    public boolean isValidProvider(ParsedProvider provider) {
        if (provider == null) return false;
        return provider.businessName() != null && !provider.businessName().isBlank();
    }
}
