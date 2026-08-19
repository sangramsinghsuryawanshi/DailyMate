package com.dailymate.jobs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobPostRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @NotBlank(message = "Category is required")
        @Size(max = 80, message = "Category must be at most 80 characters")
        String category,

        @NotBlank(message = "Location is required")
        @Size(max = 160, message = "Location must be at most 160 characters")
        String location,

        @NotBlank(message = "Type is required")
        @Size(max = 60, message = "Type must be at most 60 characters")
        String type,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description) {
}
