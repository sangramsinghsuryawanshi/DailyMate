package com.dailymate.assistant.tool.params;

public record CreateBloodRequestParams(
        String patientName,
        String bloodGroup,
        int unitsNeeded,
        String hospitalLocation,
        String urgency,
        String contactName,
        String contactPhone,
        String additionalNotes) {
}
