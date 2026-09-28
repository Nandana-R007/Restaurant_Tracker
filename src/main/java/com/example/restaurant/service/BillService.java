package com.example.restaurant.service;

import com.example.restaurant.dto.BillResponse;
import com.example.restaurant.entity.*;
import com.example.restaurant.exception.BusinessRuleException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BillService {
    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final BigDecimal taxRate;

    public BillService(BillRepository billRepository, OrderRepository orderRepository,
                       OrderItemRepository orderItemRepository, ReservationRepository reservationRepository,
                       TableRepository tableRepository,
                       @Value("${restaurant.tax.rate:0.05}") BigDecimal taxRate) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.taxRate = taxRate;
    }

    public BillResponse generate(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.BILLED || billRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessRuleException("Order " + orderId + " has already been billed.");
        }

        // Rule 6: read the items stored in the DATABASE (never trust a client-supplied total)
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        bill.setTax(tax);
        bill.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        bill.setBilledAt(LocalDateTime.now());
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill = billRepository.save(bill);

        // Rule 7: order BILLED, reservation COMPLETED, table FREE
        order.setStatus(OrderStatus.BILLED);
        orderRepository.save(order);

        Reservation sitting = order.getReservation();
        sitting.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(sitting);

        RestaurantTable table = order.getTable();
        table.setStatus(TableStatus.FREE);
        tableRepository.save(table);

        return BillResponse.from(bill, items);
    }

    public BillResponse getById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill with ID " + id + " not found"));
        return BillResponse.from(bill, orderItemRepository.findByOrderId(bill.getOrder().getId()));
    }

    public BillResponse getByOrder(Long orderId) {
        Bill bill = billRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill for order " + orderId + " not found"));
        return BillResponse.from(bill, orderItemRepository.findByOrderId(orderId));
    }
}
