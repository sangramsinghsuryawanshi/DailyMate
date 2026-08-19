package com.dailymate.emergency.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 60, message = "Category must be at most 60 characters")
        String category,

        @NotBlank(message = "Phone is required")
        @Size(max = 40, message = "Phone must be at most 40 characters")
        String phone,

        @NotBlank(message = "Location is required")
        @Size(max = 160, message = "Location must be at most 160 characters")
        String location,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
