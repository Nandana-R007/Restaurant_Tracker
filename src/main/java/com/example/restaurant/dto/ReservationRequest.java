package com.example.restaurant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationRequest(
        @NotBlank(message = "must not be blank") String customerName,
        @NotBlank(message = "must not be blank") @Size(min = 10, max = 15, message = "must be 10-15 characters") String phone,
        @NotNull(message = "is required") LocalDate reservationDate,
        @NotNull(message = "is required") LocalTime startTime,
        @NotNull(message = "is required") LocalTime endTime,
        @NotNull(message = "is required") @Positive(message = "must be greater than 0") Integer partySize,
        @NotNull(message = "is required") Long tableId) {
}
