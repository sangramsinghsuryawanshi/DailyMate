package com.dailymate.medicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record MedicineReminderRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 80, message = "Name must be at most 80 characters")
        String name,

        @NotBlank(message = "Dosage is required")
        @Size(max = 80, message = "Dosage must be at most 80 characters")
        String dosage,

        @NotBlank(message = "Frequency is required")
        @Size(max = 40, message = "Frequency must be at most 40 characters")
        String frequency,

        @NotNull(message = "Reminder time is required")
        LocalTime remindAt,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes,

        boolean active) {
}
