package com.example.restaurant.repository;

import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTableId(Long tableId);

    /** Latest order of a table that is not yet billed = the "current active order". */
    Optional<Order> findFirstByTableIdAndStatusNotOrderByOrderTimeDesc(Long tableId, OrderStatus status);
}
