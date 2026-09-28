package com.example.restaurant.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank(message = "must not be blank") String itemName,
        @NotNull(message = "is required") @Positive(message = "must be greater than 0") Integer quantity,
        @NotNull(message = "is required") @Positive(message = "must be greater than 0") BigDecimal unitPrice) {
}
