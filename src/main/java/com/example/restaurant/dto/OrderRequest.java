package com.example.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(
        @NotNull(message = "is required") Long tableId,
        Long reservationId,
        @NotEmpty(message = "must contain at least one item") @Valid List<OrderItemRequest> items) {
}
