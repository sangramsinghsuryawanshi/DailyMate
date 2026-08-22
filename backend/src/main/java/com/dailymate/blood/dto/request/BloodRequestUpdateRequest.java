package com.dailymate.blood.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BloodRequestUpdateRequest(
        @NotBlank(message = "Patient name is required")
        @Size(max = 120, message = "Patient name must be at most 120 characters")
        String patientName,

        @NotBlank(message = "Blood group is required")
        @Pattern(regexp = "^(A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-)$", message = "Blood group must be one of: A+, A-, B+, B-, AB+, AB-, O+, O-")
        String bloodGroup,

        @NotNull(message = "Units needed is required")
        @Min(value = 1, message = "Units needed must be at least 1")
        Integer unitsNeeded,

        @NotBlank(message = "Hospital location is required")
        @Size(max = 160, message = "Hospital location must be at most 160 characters")
        String hospitalLocation,

        @Pattern(regexp = "^(STANDARD|URGENT)$", message = "Urgency must be STANDARD or URGENT")
        String urgency,

        @Pattern(regexp = "^(OPEN|FULFILLED|CANCELLED)$", message = "Status must be OPEN, FULFILLED, or CANCELLED")
        String status,

        @NotBlank(message = "Contact name is required")
        @Size(max = 80, message = "Contact name must be at most 80 characters")
        String contactName,

        @NotBlank(message = "Contact phone is required")
        @Size(max = 80, message = "Contact phone must be at most 80 characters")
        String contactPhone,

        @Size(max = 1000, message = "Additional notes must be at most 1000 characters")
        String additionalNotes) {
}
