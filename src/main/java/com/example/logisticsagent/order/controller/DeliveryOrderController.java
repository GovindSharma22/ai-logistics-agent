package com.example.logisticsagent.order.controller;

import com.example.logisticsagent.order.dto.CreateDeliveryOrderRequest;
import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;

import java.util.List;

import com.example.logisticsagent.order.dto.PaginatedOrdersResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
@RestController
@RequestMapping("/api/orders")
public class DeliveryOrderController {

  private final DeliveryOrderService service;

  public DeliveryOrderController(DeliveryOrderService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DeliveryOrder create(
    @Valid @RequestBody CreateDeliveryOrderRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public DeliveryOrder getById(@PathVariable Long id) {
    return service.getById(id);
  }

  @GetMapping
  public List<DeliveryOrder> getAll() {
    return service.getAll();
  }



  @GetMapping("/page")
  public PaginatedOrdersResponse getAllPaginated(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "desc") String direction,
    @RequestParam(required = false) DeliveryStatus status,
    @RequestParam(required = false) String search) {

    if (page < 0) {
      throw new IllegalArgumentException(
        "Page must be zero or greater");
    }

    if (size < 1 || size > 100) {
      throw new IllegalArgumentException(
        "Size must be between 1 and 100");
    }

    if (!java.util.Set.of(
      "id", "orderNumber", "customerName", "status",
      "estimatedDeliveryAt", "createdAt"
    ).contains(sortBy)) {
      throw new IllegalArgumentException(
        "Invalid sort field: " + sortBy);
    }

    Sort.Direction sortDirection =
      Sort.Direction.fromString(direction);

    Pageable pageable = PageRequest.of(
      page, size, Sort.by(sortDirection, sortBy));

    Page<DeliveryOrder> result;

    if (search != null && !search.isBlank()) {
      result = service.searchOrders(search, status, pageable);
    } else if (status != null) {
      result = service.getPaginatedByStatus(status, pageable);
    } else {
      result = service.getAllPaginated(pageable);
    }

    return new PaginatedOrdersResponse(
      result.getContent(),
      result.getNumber(),
      result.getSize(),
      result.getTotalElements(),
      result.getTotalPages(),
      result.isFirst(),
      result.isLast()
    );
  }


  @GetMapping("/status/{status}")
  public List<DeliveryOrder> getByStatus(
    @PathVariable DeliveryStatus status) {
    return service.getByStatus(status);
  }


  @PatchMapping("/{id}/status")
  public DeliveryOrder updateStatus(
    @PathVariable Long id,
    @RequestParam DeliveryStatus status) {
    return service.updateStatus(id, status);
  }
  @GetMapping("/delayed")
  public List<DeliveryOrder> getDelayedOrders() {
    return service.getDelayedOrders();
  }
  @GetMapping("/metrics")
  public DeliveryMetricsResponse getDeliveryMetrics() {
    return service.getDeliveryMetrics();
  }
  @GetMapping("/risks")
  public List<DeliveryRiskResponse> getDeliveryRisks() {
    return service.getDeliveryRisks();
  }
}
