
package com.example.logisticsagent.order.service;

import com.example.logisticsagent.order.dto.CreateDeliveryOrderRequest;
import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import com.example.logisticsagent.order.exception.DuplicateOrderException;
import com.example.logisticsagent.order.exception.InvalidStatusTransitionException;
import com.example.logisticsagent.order.exception.OrderNotFoundException;
import com.example.logisticsagent.order.repository.DeliveryOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
@Service
public class DeliveryOrderService {

  private final DeliveryOrderRepository repository;
  public DeliveryOrderService(DeliveryOrderRepository repository) {
    this.repository = repository;
  }
  private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED_TRANSITIONS =
    Map.of(
      DeliveryStatus.CREATED, Set.of(
        DeliveryStatus.CONFIRMED,
        DeliveryStatus.CANCELLED
      ),
      DeliveryStatus.CONFIRMED, Set.of(
        DeliveryStatus.PICKING,
        DeliveryStatus.CANCELLED
      ),
      DeliveryStatus.PICKING, Set.of(
        DeliveryStatus.READY_FOR_DISPATCH,
        DeliveryStatus.CANCELLED
      ),
      DeliveryStatus.READY_FOR_DISPATCH, Set.of(
        DeliveryStatus.OUT_FOR_DELIVERY,
        DeliveryStatus.CANCELLED
      ),
      DeliveryStatus.OUT_FOR_DELIVERY, Set.of(
        DeliveryStatus.DELIVERED,
        DeliveryStatus.DELAYED
      ),
      DeliveryStatus.DELAYED, Set.of(
        DeliveryStatus.OUT_FOR_DELIVERY,
        DeliveryStatus.CANCELLED
      ),
      DeliveryStatus.DELIVERED, Set.of(),
      DeliveryStatus.CANCELLED, Set.of()
    );



  @Transactional
  public DeliveryOrder create(CreateDeliveryOrderRequest request) {
    if (repository.existsByOrderNumber(request.orderNumber())) {
      throw new DuplicateOrderException(
        "Order number already exists: " + request.orderNumber()
      );
    }

    DeliveryOrder order = new DeliveryOrder(
      request.orderNumber(),
      request.customerName(),
      request.deliveryAddress(),
      request.estimatedDeliveryAt()
    );

    return repository.save(order);
  }

  @Transactional(readOnly = true)
  public DeliveryOrder getById(Long id) {
    return repository.findById(id)
      .orElseThrow(() -> new OrderNotFoundException(
        "Delivery order not found: " + id
      ));
  }

  @Transactional(readOnly = true)
  public List<DeliveryOrder> getAll() {
    return repository.findAll();
  }
  @Transactional(readOnly = true)
  public Page<DeliveryOrder> getAllPaginated(Pageable pageable) {
    return repository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Page<DeliveryOrder> getPaginatedByStatus(
    DeliveryStatus status,
    Pageable pageable) {

    return repository.findByStatus(status, pageable);
  }


  @Transactional(readOnly = true)
  public List<DeliveryOrder> getByStatus(DeliveryStatus status) {
    return repository.findByStatus(status);
  }

  @Transactional
  public DeliveryOrder updateStatus(Long id, DeliveryStatus newStatus) {
    DeliveryOrder order = repository.findById(id)
      .orElseThrow(() -> new OrderNotFoundException(
        "Delivery order not found: " + id
      ));

    DeliveryStatus currentStatus = order.getStatus();

    if (!ALLOWED_TRANSITIONS
      .getOrDefault(currentStatus, Set.of())
      .contains(newStatus)) {
      throw new InvalidStatusTransitionException(
        "Cannot change order status from "
          + currentStatus + " to " + newStatus
      );
    }

    if (newStatus == DeliveryStatus.DELIVERED) {
      order.markDelivered(LocalDateTime.now());
    } else {
      order.updateStatus(newStatus);
    }

    return repository.save(order);
  }

  @Transactional(readOnly = true)
  public List<DeliveryOrder> getDelayedOrders() {
    return repository.findByEstimatedDeliveryAtBeforeAndStatusNotIn(
      LocalDateTime.now(),
      List.of(
        DeliveryStatus.DELIVERED,
        DeliveryStatus.CANCELLED
      )
    );
  }

  @Transactional(readOnly = true)
  public DeliveryMetricsResponse getDeliveryMetrics() {
    long totalOrders = repository.count();

    long deliveredOrders =
      repository.countByStatus(DeliveryStatus.DELIVERED);

    long delayedOrders = getDelayedOrders().size();

    long onTimeOrders =
      repository.countDeliveredOnTime();

    BigDecimal onTimeRate = deliveredOrders == 0
      ? BigDecimal.ZERO.setScale(2)
      : BigDecimal.valueOf(onTimeOrders)
      .multiply(BigDecimal.valueOf(100))
      .divide(
        BigDecimal.valueOf(deliveredOrders),
        2,
        RoundingMode.HALF_UP
      );
    long overdueUndeliveredOrders =
      repository.countOverdueUndelivered(LocalDateTime.now());

    return new DeliveryMetricsResponse(
      totalOrders,
      deliveredOrders,
      delayedOrders,
      onTimeRate,
      overdueUndeliveredOrders
    );
  }

  @Transactional(readOnly = true)
  public Page<DeliveryOrder> searchByOrderNumber(
    String orderNumber,
    Pageable pageable
  ) {
    return repository.findByOrderNumberContainingIgnoreCase(
      orderNumber, pageable
    );
  }

  @Transactional(readOnly = true)
  public Page<DeliveryOrder> searchByCustomerName(
    String customerName,
    Pageable pageable
  ) {
    return repository.findByCustomerNameContainingIgnoreCase(
      customerName, pageable
    );
  }

  @Transactional(readOnly = true)
  public Page<DeliveryOrder> searchOrders(
    String search,
    DeliveryStatus status,
    Pageable pageable
  ) {
    return repository.searchOrders(
      search.trim(),
      status,
      pageable
    );
  }


  @Transactional(readOnly = true)
  public List<DeliveryRiskResponse> getDeliveryRisks() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime warningTime = now.plusMinutes(60);

    return repository.findAll().stream()
      .filter(order ->
        order.getStatus() != DeliveryStatus.DELIVERED
          && order.getStatus() != DeliveryStatus.CANCELLED
      )
      .filter(order ->
        !order.getEstimatedDeliveryAt().isAfter(warningTime)
      )
      .map(order -> {
        LocalDateTime eta = order.getEstimatedDeliveryAt();

        String riskLevel;
        String reason;

        if (eta.isBefore(now)) {
          riskLevel = "HIGH";
          reason = "Estimated delivery time has passed.";
        } else {
          riskLevel = "MEDIUM";
          reason = "Estimated delivery is within the next 60 minutes.";
        }

        return new DeliveryRiskResponse(
          order.getId(),
          order.getOrderNumber(),
          order.getCustomerName(),
          order.getStatus().name(),
          eta,
          riskLevel,
          reason
        );
      })
      .toList();
  }





}
