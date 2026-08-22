package com.dailymate.assistant.tool.params;

import java.time.LocalDate;

public record CreateEventParams(
        String title,
        String description,
        String location,
        LocalDate eventDate,
        String category) {
}
