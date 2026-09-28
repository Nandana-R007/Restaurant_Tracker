package com.example.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank
    private String itemName;

    @NotNull
    @Positive
    private Integer quantity;

    @NotNull
    @Positive
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order v) { this.order = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
}
