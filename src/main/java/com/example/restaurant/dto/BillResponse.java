package com.example.restaurant.dto;

import com.example.restaurant.dto.Responses.OrderItemResponse;
import com.example.restaurant.entity.Bill;
import com.example.restaurant.entity.OrderItem;
import com.example.restaurant.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(Long billId, Long orderId, String tableNumber, List<OrderItemResponse> items,
                           BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                           LocalDateTime billedAt, PaymentStatus paymentStatus) {

    public static BillResponse from(Bill b, List<OrderItem> items) {
        return new BillResponse(b.getId(), b.getOrder().getId(), b.getOrder().getTable().getTableNumber(),
                items.stream().map(OrderItemResponse::from).toList(),
                b.getSubtotal(), b.getTax(), b.getTotal(), b.getBilledAt(), b.getPaymentStatus());
    }
}
