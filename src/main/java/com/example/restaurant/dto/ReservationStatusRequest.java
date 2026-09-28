package com.example.restaurant.dto;

import com.example.restaurant.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusRequest(@NotNull(message = "is required") ReservationStatus status) {
}
