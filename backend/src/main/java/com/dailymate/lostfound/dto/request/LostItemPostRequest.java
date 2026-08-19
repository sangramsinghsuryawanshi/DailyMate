package com.dailymate.lostfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LostItemPostRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Item type is required")
        @Size(max = 80, message = "Item type must be at most 80 characters")
        String itemType,

        @NotBlank(message = "Location is required")
        @Size(max = 160, message = "Location must be at most 160 characters")
        String location,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotBlank(message = "Contact name is required")
        @Size(max = 80, message = "Contact name must be at most 80 characters")
        String contactName,

        @NotBlank(message = "Contact phone is required")
        @Size(max = 80, message = "Contact phone must be at most 80 characters")
        String contactPhone) {
}
