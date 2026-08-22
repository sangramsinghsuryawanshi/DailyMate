package com.dailymate.marketplace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ServiceProviderRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 80, message = "Name must be at most 80 characters")
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 40, message = "Category must be at most 40 characters")
        String category,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotBlank(message = "Service area is required")
        @Size(max = 120, message = "Service area must be at most 120 characters")
        String serviceArea,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email,

        @DecimalMin(value = "0.0", inclusive = true, message = "Hourly rate must be non-negative")
        BigDecimal hourlyRate) {
}
