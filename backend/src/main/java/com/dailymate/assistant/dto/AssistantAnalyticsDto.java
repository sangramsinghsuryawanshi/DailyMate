package com.dailymate.assistant.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

public final class AssistantAnalyticsDto {

    private AssistantAnalyticsDto() {}

    public enum ChangeType {
        POSITIVE_GROWTH,
        NEGATIVE_GROWTH,
        NEW_SPEND,
        NO_CHANGE,
        COMPLETE_REDUCTION
    }

    public record PeriodExpenseSummary(
            YearMonth period,
            BigDecimal authoritativeTotal,
            long transactionCount,
            BigDecimal averageTransaction
    ) {}

    public record PeriodComparisonDto(
            YearMonth period1,
            BigDecimal total1,
            YearMonth period2,
            BigDecimal total2,
            BigDecimal deltaAmount,
            BigDecimal percentageChange,
            ChangeType changeType
    ) {}

    public record CategoryBreakdownItem(
            String category,
            BigDecimal rawTotal,
            long count
    ) {}

    public record MonthlyAnalyticsReport(
            YearMonth period,
            BigDecimal authoritativeTotal,
            List<CategoryBreakdownItem> categories,
            PeriodComparisonDto momComparison,
            int activeRemindersCount,
            int emergencyContactCount,
            long unreadNotificationsCount,
            Instant generatedAt
    ) {}

    public record ReportExportResult(
            String filename,
            String contentType,
            byte[] content
    ) {}
}
