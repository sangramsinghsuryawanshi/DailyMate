package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.emergency.service.EmergencyContactService;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.medicine.service.MedicineReminderService;
import com.dailymate.notification.service.NotificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Deterministic Domain Analytics Service for DailyMate AI Assistant.
 * Invariant: Authoritative computations only with BigDecimal and half-open date intervals.
 * Zero repository injection to preserve architecture governance boundary.
 */
@Service
public class AssistantAnalyticsService {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private final ExpenseService expenseService;
    private final MedicineReminderService reminderService;
    private final EmergencyContactService emergencyService;
    private final NotificationService notificationService;

    public AssistantAnalyticsService(
            ExpenseService expenseService,
            MedicineReminderService reminderService,
            EmergencyContactService emergencyService,
            NotificationService notificationService) {
        this.expenseService = expenseService;
        this.reminderService = reminderService;
        this.emergencyService = emergencyService;
        this.notificationService = notificationService;
    }

    public PeriodExpenseSummary getPeriodSummary(String userId, YearMonth period) {
        List<ExpenseEntryResponse> periodEntries = getEntriesForPeriod(userId, period);
        BigDecimal total = periodEntries.stream()
                .map(ExpenseEntryResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = periodEntries.size();
        BigDecimal avg = count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new PeriodExpenseSummary(period, total, count, avg);
    }

    public PeriodComparisonDto comparePeriods(String userId, YearMonth period1, YearMonth period2) {
        PeriodExpenseSummary sum1 = getPeriodSummary(userId, period1);
        PeriodExpenseSummary sum2 = getPeriodSummary(userId, period2);

        BigDecimal total1 = sum1.authoritativeTotal();
        BigDecimal total2 = sum2.authoritativeTotal();
        BigDecimal delta = total2.subtract(total1);

        BigDecimal percentageChange;
        ChangeType changeType;

        if (total1.compareTo(BigDecimal.ZERO) == 0 && total2.compareTo(BigDecimal.ZERO) == 0) {
            percentageChange = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            changeType = ChangeType.NO_CHANGE;
        } else if (total1.compareTo(BigDecimal.ZERO) == 0 && total2.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = null; // Prevent Infinity
            changeType = ChangeType.NEW_SPEND;
        } else if (total1.compareTo(BigDecimal.ZERO) > 0 && total2.compareTo(BigDecimal.ZERO) == 0) {
            percentageChange = new BigDecimal("-100.00");
            changeType = ChangeType.COMPLETE_REDUCTION;
        } else {
            percentageChange = delta.multiply(new BigDecimal("100"))
                    .divide(total1, 2, RoundingMode.HALF_UP);

            if (percentageChange.compareTo(BigDecimal.ZERO) > 0) {
                changeType = ChangeType.POSITIVE_GROWTH;
            } else if (percentageChange.compareTo(BigDecimal.ZERO) < 0) {
                changeType = ChangeType.NEGATIVE_GROWTH;
            } else {
                changeType = ChangeType.NO_CHANGE;
            }
        }

        return new PeriodComparisonDto(period1, total1, period2, total2, delta, percentageChange, changeType);
    }

    public List<CategoryBreakdownItem> getCategoryBreakdown(String userId, YearMonth period) {
        List<ExpenseEntryResponse> periodEntries = getEntriesForPeriod(userId, period);
        Map<String, List<ExpenseEntryResponse>> grouped = periodEntries.stream()
                .collect(Collectors.groupingBy(ExpenseEntryResponse::category));

        List<CategoryBreakdownItem> result = new ArrayList<>();
        for (Map.Entry<String, List<ExpenseEntryResponse>> entry : grouped.entrySet()) {
            BigDecimal rawTotal = entry.getValue().stream()
                    .map(ExpenseEntryResponse::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new CategoryBreakdownItem(entry.getKey(), rawTotal, entry.getValue().size()));
        }

        result.sort((a, b) -> b.rawTotal().compareTo(a.rawTotal()));
        return result;
    }

    public MonthlyAnalyticsReport generateMonthlyReport(String userId, YearMonth period) {
        PeriodExpenseSummary currentSummary = getPeriodSummary(userId, period);
        List<CategoryBreakdownItem> categories = getCategoryBreakdown(userId, period);

        YearMonth previousMonth = period.minusMonths(1);
        PeriodComparisonDto momComparison = comparePeriods(userId, previousMonth, period);

        int activeReminders = reminderService.getReminders(userId).size();
        int iceContacts = emergencyService.getMyContacts(userId, null).size();
        long unreadNotifications = notificationService.getNotifications(userId).stream()
                .filter(n -> !n.read())
                .count();

        return new MonthlyAnalyticsReport(
                period,
                currentSummary.authoritativeTotal(),
                categories,
                momComparison,
                activeReminders,
                iceContacts,
                unreadNotifications,
                Instant.now()
        );
    }

    public List<ExpenseEntryResponse> getEntriesForPeriod(String userId, YearMonth period) {
        LocalDate startInclusive = period.atDay(1);
        LocalDate nextPeriodStartExclusive = period.plusMonths(1).atDay(1);

        return expenseService.getEntries(userId).stream()
                .filter(e -> e.spentOn() != null
                        && !e.spentOn().isBefore(startInclusive)
                        && e.spentOn().isBefore(nextPeriodStartExclusive))
                .toList();
    }
}
