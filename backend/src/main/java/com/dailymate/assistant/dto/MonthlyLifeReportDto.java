package com.dailymate.assistant.dto;

import java.math.BigDecimal;
import java.util.Map;

public record MonthlyLifeReportDto(
        BigDecimal monthlyExpenseTotal,
        Map<String, BigDecimal> expenseCategoryTotals,
        int activeRemindersCount,
        String bloodDonorStatus,
        int unreadNotificationCount,
        int emergencyContactCount,
        String generatedAt) {
}
