package com.example.restaurant.controller;

import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.service.TableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tables")
public class TableController {
    private final TableService tableService;

    public TableController(TableService tableService) { this.tableService = tableService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantTable create(@Valid @RequestBody RestaurantTable table) { return tableService.create(table); }

    @GetMapping
    public List<RestaurantTable> getAll() { return tableService.getAll(); }

    @GetMapping("/available")
    public List<RestaurantTable> getAvailable() { return tableService.getAvailable(); }

    @GetMapping("/{id}")
    public RestaurantTable getOne(@PathVariable Long id) { return tableService.getById(id); }

    @PutMapping("/{id}")
    public RestaurantTable update(@PathVariable Long id, @Valid @RequestBody RestaurantTable table) {
        return tableService.update(id, table);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { tableService.delete(id); }
}
