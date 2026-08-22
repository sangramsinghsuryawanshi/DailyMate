package com.dailymate.assistant.tool.params;

import java.math.BigDecimal;

public record CreateJobParams(
        String title,
        String companyName,
        String location,
        String type,
        String description,
        BigDecimal salary,
        String contactEmail) {
}
