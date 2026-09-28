package com.example.restaurant.repository;

import com.example.restaurant.entity.RestaurantTable;
import com.example.restaurant.entity.TableStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findByStatus(TableStatus status);
    boolean existsByTableNumber(String tableNumber);
}
