package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.MonthlyLifeReportDto;
import com.dailymate.blood.service.BloodDonationService;
import com.dailymate.emergency.service.EmergencyContactService;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.medicine.service.MedicineReminderService;
import com.dailymate.notification.service.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Deterministic life reporting service.
 * Invariant: Never allows LLM/AI to fabricate, estimate, or hallucinate summary calculations.
 */
@Service
public class AssistantReportingService {

    private final ExpenseService expenseService;
    private final MedicineReminderService reminderService;
    private final BloodDonationService bloodService;
    private final NotificationService notificationService;
    private final EmergencyContactService emergencyService;

    public AssistantReportingService(
            ExpenseService expenseService,
            MedicineReminderService reminderService,
            BloodDonationService bloodService,
            NotificationService notificationService,
            EmergencyContactService emergencyService) {
        this.expenseService = expenseService;
        this.reminderService = reminderService;
        this.bloodService = bloodService;
        this.notificationService = notificationService;
        this.emergencyService = emergencyService;
    }

    public MonthlyLifeReportDto generateMonthlyReport(String userId) {
        // 1. Calculate Expenses
        var entries = expenseService.getEntries(userId);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        if (entries != null) {
            for (var e : entries) {
                total = total.add(e.amount());
                categoryTotals.merge(e.category(), e.amount(), BigDecimal::add);
            }
        }

        // 2. Count Active Reminders
        var reminders = reminderService.getReminders(userId);
        int activeReminders = 0;
        if (reminders != null) {
            activeReminders = (int) reminders.stream().filter(r -> Boolean.TRUE.equals(r.active())).count();
        }

        // 3. User Blood Requests Status
        String donorStatus = "NO_ACTIVE_REQUESTS";
        try {
            var requests = bloodService.getMyRequests(userId);
            if (requests != null && !requests.isEmpty()) {
                donorStatus = requests.size() + " Active Request(s)";
            }
        } catch (Exception ignored) {}

        // 4. Unread Notifications Count
        int unreadNotifs = 0;
        try {
            var notifs = notificationService.getNotifications(userId);
            if (notifs != null) {
                unreadNotifs = (int) notifs.stream().filter(n -> !Boolean.TRUE.equals(n.read())).count();
            }
        } catch (Exception ignored) {}

        // 5. Emergency Contacts Count
        int iceContacts = 0;
        try {
            var contacts = emergencyService.getMyContacts(userId, null);
            if (contacts != null) {
                iceContacts = contacts.size();
            }
        } catch (Exception ignored) {}

        return new MonthlyLifeReportDto(
                total,
                categoryTotals,
                activeReminders,
                donorStatus,
                unreadNotifs,
                iceContacts,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        );
    }
}
