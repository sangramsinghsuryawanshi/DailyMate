package com.dailymate.expense.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseEntryResponse(
        String id,
        String category,
        String description,
        BigDecimal amount,
        LocalDate spentOn,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
