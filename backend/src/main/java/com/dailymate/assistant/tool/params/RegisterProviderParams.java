package com.dailymate.assistant.tool.params;

public record RegisterProviderParams(
        String name,
        String serviceType,
        String phone,
        String city,
        String area,
        int experienceYears) {
}
