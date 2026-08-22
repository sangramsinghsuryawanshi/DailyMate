package com.dailymate.assistant.tool.params;

public record CreateIceContactParams(
        String name,
        String relationship,
        String phone,
        String category,
        String notes) {
}
