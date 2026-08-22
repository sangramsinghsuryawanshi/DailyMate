package com.dailymate.assistant.tool;

import java.time.LocalTime;

public record CreateReminderParams(
        String name,
        String dosage,
        String frequency,
        LocalTime remindAt,
        String notes,
        boolean active) {
}
