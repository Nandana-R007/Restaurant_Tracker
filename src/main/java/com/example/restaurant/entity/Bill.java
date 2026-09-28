package com.example.restaurant.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(precision = 10, scale = 2) private BigDecimal subtotal;
    @Column(precision = 10, scale = 2) private BigDecimal tax;
    @Column(precision = 10, scale = 2) private BigDecimal total;
    private LocalDateTime billedAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order v) { this.order = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal v) { this.tax = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
    public LocalDateTime getBilledAt() { return billedAt; }
    public void setBilledAt(LocalDateTime v) { this.billedAt = v; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus v) { this.paymentStatus = v; }
}
