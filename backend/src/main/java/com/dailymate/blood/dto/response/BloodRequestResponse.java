package com.dailymate.blood.dto.response;

import java.time.Instant;

public record BloodRequestResponse(
        String id,
        String userId,
        String patientName,
        String bloodGroup,
        int unitsNeeded,
        String hospitalLocation,
        String urgency,
        String status,
        String contactName,
        String contactPhone,
        String additionalNotes,
        Instant createdAt,
        Instant updatedAt) {
}
