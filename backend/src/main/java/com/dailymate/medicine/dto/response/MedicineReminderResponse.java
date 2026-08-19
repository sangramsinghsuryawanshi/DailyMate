package com.dailymate.medicine.dto.response;

import java.time.Instant;
import java.time.LocalTime;

public record MedicineReminderResponse(
        String id,
        String name,
        String dosage,
        String frequency,
        LocalTime remindAt,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
