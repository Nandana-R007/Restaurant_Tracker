package com.example.restaurant.service;

import com.example.restaurant.dto.OrderItemRequest;
import com.example.restaurant.dto.OrderRequest;
import com.example.restaurant.dto.Responses.OrderResponse;
import com.example.restaurant.entity.*;
import com.example.restaurant.exception.BusinessRuleException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.OrderRepository;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.TableRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public OrderService(OrderRepository orderRepository, TableRepository tableRepository,
                        ReservationRepository reservationRepository) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    public OrderResponse create(OrderRequest req) {
        // 1. table must exist
        RestaurantTable table = tableRepository.findById(req.tableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table with ID " + req.tableId() + " not found"));

        // 2. Rule 4: table must be occupied (seated)
        if (table.getStatus() != TableStatus.OCCUPIED) {
            throw new BusinessRuleException("Cannot place order because the table is not currently seated.");
        }

        // 3. there must be a SEATED reservation (sitting) at this table
        Reservation sitting = reservationRepository.findFirstByTableIdAndStatus(table.getId(), ReservationStatus.SEATED)
                .orElseThrow(() -> new BusinessRuleException("Cannot place order because the table has no active seated reservation."));
        if (req.reservationId() != null && !req.reservationId().equals(sitting.getId())) {
            throw new BusinessRuleException("Reservation " + req.reservationId() + " is not the active sitting of table "
                    + table.getTableNumber() + ".");
        }

        // 4. build the order with its items (Rule 5)
        Order order = new Order();
        order.setTable(table);
        order.setReservation(sitting);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);
        for (OrderItemRequest ir : req.items()) {
            OrderItem item = new OrderItem();
            item.setItemName(ir.itemName().trim());
            item.setQuantity(ir.quantity());
            item.setUnitPrice(ir.unitPrice());
            order.addItem(item);
        }
        return OrderResponse.from(orderRepository.save(order));   // cascade saves the items too
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    public OrderResponse getById(Long id) { return OrderResponse.from(findOrder(id)); }

    public List<OrderResponse> getByTable(Long tableId) {
        if (!tableRepository.existsById(tableId)) {
            throw new ResourceNotFoundException("Table with ID " + tableId + " not found");
        }
        return orderRepository.findByTableId(tableId).stream().map(OrderResponse::from).toList();
    }

    /** Allowed: PLACED -> PREPARING -> READY -> SERVED (one step at a time). BILLED is set only by billing. */
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        Order order = findOrder(id);
        OrderStatus current = order.getStatus();
        if (next == OrderStatus.BILLED) {
            throw new BusinessRuleException("An order becomes BILLED only when its bill is generated.");
        }
        if (current == OrderStatus.BILLED || next.ordinal() != current.ordinal() + 1) {
            throw new BusinessRuleException("Invalid order status change from " + current + " to " + next + ".");
        }
        order.setStatus(next);
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found"));
    }
}
