package com.dailymate.assistant.tool.params;

public record RegisterDonorParams(
        String bloodGroup,
        String phone,
        String city,
        String area,
        String notes,
        boolean available) {
}
