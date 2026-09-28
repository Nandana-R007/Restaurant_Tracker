package com.example.restaurant.service;

import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.entity.TableStatus;
import com.example.restaurant.exception.BusinessRuleException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.ReservationRepository;
import com.example.restaurant.repository.TableRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TableService {
    private final TableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public TableService(TableRepository tableRepository, ReservationRepository reservationRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    public RestaurantTable create(RestaurantTable table) {
        if (tableRepository.existsByTableNumber(table.getTableNumber())) {
            throw new BusinessRuleException("Table " + table.getTableNumber() + " already exists.");
        }
        table.setStatus(TableStatus.FREE);      // new tables always start FREE
        return tableRepository.save(table);
    }

    public List<RestaurantTable> getAll() { return tableRepository.findAll(); }

    public List<RestaurantTable> getAvailable() { return tableRepository.findByStatus(TableStatus.FREE); }

    public RestaurantTable getById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table with ID " + id + " not found"));
    }

    public RestaurantTable update(Long id, RestaurantTable data) {
        RestaurantTable table = getById(id);
        if (!table.getTableNumber().equals(data.getTableNumber()) && tableRepository.existsByTableNumber(data.getTableNumber())) {
            throw new BusinessRuleException("Table " + data.getTableNumber() + " already exists.");
        }
        table.setTableNumber(data.getTableNumber());
        table.setCapacity(data.getCapacity());   // status is NOT changed here
        return tableRepository.save(table);
    }

    public void delete(Long id) {
        RestaurantTable table = getById(id);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessRuleException("Table " + table.getTableNumber() + " is currently occupied and cannot be deleted.");
        }
        if (reservationRepository.existsByTableId(id)) {
            throw new BusinessRuleException("Table " + table.getTableNumber() + " has reservation history and cannot be deleted.");
        }
        tableRepository.delete(table);
    }
}
