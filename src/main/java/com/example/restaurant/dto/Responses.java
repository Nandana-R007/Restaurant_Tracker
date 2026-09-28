package com.example.restaurant.dto;

import com.example.restaurant.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Response DTOs: flat objects, so JSON never recurses through bidirectional relations. */
public final class Responses {
    private Responses() { }

    public record ReservationResponse(Long id, String customerName, String phone, LocalDate reservationDate,
                                      LocalTime startTime, LocalTime endTime, Integer partySize,
                                      Long tableId, String tableNumber, ReservationStatus status) {
        public static ReservationResponse from(Reservation r) {
            return new ReservationResponse(r.getId(), r.getCustomerName(), r.getPhone(), r.getReservationDate(),
                    r.getStartTime(), r.getEndTime(), r.getPartySize(), r.getTable().getId(),
                    r.getTable().getTableNumber(), r.getStatus());
        }
    }

    public record OrderItemResponse(Long id, String itemName, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getId(), i.getItemName(), i.getQuantity(), i.getUnitPrice(),
                    i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
    }

    public record OrderResponse(Long id, Long tableId, String tableNumber, Long reservationId,
                                LocalDateTime orderTime, OrderStatus status, List<OrderItemResponse> items) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getTable().getId(), o.getTable().getTableNumber(),
                    o.getReservation().getId(), o.getOrderTime(), o.getStatus(),
                    o.getItems().stream().map(OrderItemResponse::from).toList());
        }
    }
}
