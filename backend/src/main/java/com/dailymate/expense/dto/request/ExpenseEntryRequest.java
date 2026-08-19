package com.dailymate.expense.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseEntryRequest(
        @NotBlank(message = "Category is required")
        @Size(max = 40, message = "Category must be at most 40 characters")
        String category,

        @NotBlank(message = "Description is required")
        @Size(max = 120, message = "Description must be at most 120 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Expense date is required")
        LocalDate spentOn,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes) {
}
