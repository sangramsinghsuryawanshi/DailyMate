package com.dailymate.assistant.tool;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordExpenseParams(
        String category,
        String description,
        BigDecimal amount,
        LocalDate spentOn,
        String notes) {
}
