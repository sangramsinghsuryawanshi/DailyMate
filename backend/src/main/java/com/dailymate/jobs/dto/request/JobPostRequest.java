package com.dailymate.jobs.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

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

        @DecimalMin(value = "0.00", message = "Salary cannot be negative")
        BigDecimal salary,

        @Size(max = 120, message = "Company name must be at most 120 characters")
        String companyName,

        @Size(max = 20, message = "Contact phone must be at most 20 characters")
        String contactPhone,

        @Size(max = 120, message = "Contact email must be at most 120 characters")
        String contactEmail,

        String status,

        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description) {
}
