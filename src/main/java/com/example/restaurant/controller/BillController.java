package com.example.restaurant.controller;

import com.example.restaurant.dto.BillResponse;
import com.example.restaurant.service.BillService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BillController {
    private final BillService billService;

    public BillController(BillService billService) { this.billService = billService; }

    @PostMapping("/bills/order/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BillResponse generate(@PathVariable Long orderId) { return billService.generate(orderId); }

    @GetMapping("/bills/{id}")
    public BillResponse getOne(@PathVariable Long id) { return billService.getById(id); }

    @GetMapping("/orders/{orderId}/bill")
    public BillResponse getByOrder(@PathVariable Long orderId) { return billService.getByOrder(orderId); }
}
