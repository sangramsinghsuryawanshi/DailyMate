package com.dailymate.assistant.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AssistantContext(
        List<ReminderContext> reminders,
        ExpenseContext expenses,
        List<EventContext> events,
        List<JobContext> jobs,
        EmergencyContext emergency) {

    public record ReminderContext(
            String medicineName,
            String dosage,
            String scheduledTime,
            String frequency) {
    }

    public record ExpenseContext(
            BigDecimal monthlyTotal,
            Map<String, BigDecimal> categoryTotals,
            int count) {
    }

    public record EventContext(
            String title,
            String category,
            String location,
            String eventDate) {
    }

    public record JobContext(
            String title,
            String type,
            String location,
            BigDecimal salary,
            String companyName) {
    }

    public record EmergencyContext(
            List<String> primaryHotlines,
            int personalContactCount) {
    }
}
