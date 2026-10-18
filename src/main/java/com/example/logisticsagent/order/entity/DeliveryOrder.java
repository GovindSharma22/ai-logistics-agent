package com.example.logisticsagent.order.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "delivery_orders",
  indexes = {
    @Index(name = "idx_delivery_status", columnList = "status"),
    @Index(name = "idx_delivery_eta", columnList = "estimated_delivery_at")
  }
)
public class DeliveryOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_number", nullable = false, unique = true, length = 50)
  private String orderNumber;

  @Column(name = "customer_name", nullable = false, length = 100)
  private String customerName;

  @Column(name = "delivery_address", nullable = false, length = 500)
  private String deliveryAddress;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DeliveryStatus status;

  @Column(name = "estimated_delivery_at", nullable = false)
  private LocalDateTime estimatedDeliveryAt;

  @Column(name = "actual_delivery_at")
  private LocalDateTime actualDeliveryAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected DeliveryOrder() {
  }

  public DeliveryOrder(
    String orderNumber,
    String customerName,
    String deliveryAddress,
    LocalDateTime estimatedDeliveryAt) {
    this.orderNumber = orderNumber;
    this.customerName = customerName;
    this.deliveryAddress = deliveryAddress;
    this.estimatedDeliveryAt = estimatedDeliveryAt;
    this.status = DeliveryStatus.CREATED;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public DeliveryStatus getStatus() {
    return status;
  }

  public LocalDateTime getEstimatedDeliveryAt() {
    return estimatedDeliveryAt;
  }

  public LocalDateTime getActualDeliveryAt() {
    return actualDeliveryAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void updateStatus(DeliveryStatus status) {
    this.status = status;
  }

  public void markDelivered(LocalDateTime actualDeliveryAt) {
    this.status = DeliveryStatus.DELIVERED;
    this.actualDeliveryAt = actualDeliveryAt;
  }
}
