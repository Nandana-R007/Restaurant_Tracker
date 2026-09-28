package com.example.restaurant.dto;

import com.example.restaurant.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull(message = "is required") OrderStatus status) {
}
