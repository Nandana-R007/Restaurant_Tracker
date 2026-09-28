package com.example.restaurant.controller;

import com.example.restaurant.dto.OrderRequest;
import com.example.restaurant.dto.OrderStatusRequest;
import com.example.restaurant.dto.Responses.OrderResponse;
import com.example.restaurant.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request) { return orderService.create(request); }

    @GetMapping("/orders")
    public List<OrderResponse> getAll() { return orderService.getAll(); }

    @GetMapping("/orders/{id}")
    public OrderResponse getOne(@PathVariable Long id) { return orderService.getById(id); }

    @GetMapping("/tables/{tableId}/orders")
    public List<OrderResponse> getByTable(@PathVariable Long tableId) { return orderService.getByTable(tableId); }

    @PutMapping("/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
