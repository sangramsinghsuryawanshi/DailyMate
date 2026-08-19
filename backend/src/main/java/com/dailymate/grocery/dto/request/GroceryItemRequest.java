package com.dailymate.grocery.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroceryItemRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 80, message = "Category must be at most 80 characters")
        String category,

        @NotBlank(message = "Store is required")
        @Size(max = 80, message = "Store must be at most 80 characters")
        String store,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        Double price,

        @NotBlank(message = "Location is required")
        @Size(max = 160, message = "Location must be at most 160 characters")
        String location) {
}
