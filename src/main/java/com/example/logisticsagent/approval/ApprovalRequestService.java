
package com.example.logisticsagent.approval;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import com.example.logisticsagent.order.service.DeliveryOrderService;

@Service
public class ApprovalRequestService {

  private final ApprovalRequestRepository repository;
  private final DeliveryOrderService deliveryOrderService;
  private final ApprovalExecutionAuditService auditService;
  public ApprovalRequestService(
    ApprovalRequestRepository repository,
    DeliveryOrderService deliveryOrderService,
    ApprovalExecutionAuditService auditService) {
    this.repository = repository;
    this.deliveryOrderService = deliveryOrderService;
    this.auditService = auditService;
  }

  @Transactional
  public ApprovalRequest create(
    Long orderId,
    String orderNumber,
    String recommendedAction,
    String explanation) {

    if (!"Move order to OUT_FOR_DELIVERY".equals(recommendedAction)) {
      throw new IllegalArgumentException(
        "This approval action is not supported");
    }

    DeliveryOrder order = deliveryOrderService.getById(orderId);

    if (!order.getOrderNumber().equals(orderNumber)) {
      throw new IllegalArgumentException(
        "Approval order number does not match the delivery order");
    }

    if (order.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
      throw new IllegalStateException(
        "Order is already OUT_FOR_DELIVERY; no approval is needed");
    }

    ApprovalRequest request = new ApprovalRequest(
      orderId,
      orderNumber,
      recommendedAction,
      explanation
    );

    return repository.save(request);
  }

  @Transactional(readOnly = true)
  public List<ApprovalRequest> getPendingApprovals() {
    return repository.findByStatusOrderByCreatedAtDesc(
      ApprovalStatus.PENDING
    );
  }

  @Transactional(readOnly = true)
  public List<ApprovalRequest> getOrderHistory(Long orderId) {
    return repository.findByOrderIdOrderByCreatedAtDesc(orderId);
  }

  @Transactional
  public ApprovalRequest approve(Long id, String username) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    request.approve(username);
    return repository.save(request);
  }

  @Transactional
  public ApprovalRequest reject(Long id, String username) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    request.reject(username);
    return repository.save(request);
  }

  @Transactional
  public ApprovalRequest markExecuted(Long id) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    request.markExecuted("demo-user");
    return repository.save(request);
  }

  @Transactional
  public ApprovalRequest markFailed(Long id) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    request.markFailed("Execution failed", "demo-user");
    return repository.save(request);
  }


  @Transactional
  public ApprovalRequest executeApprovedAction(Long id) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    if (request.getStatus() != ApprovalStatus.APPROVED) {
      throw new IllegalStateException(
        "Only approved requests can be executed");
    }

    if (!"Move order to OUT_FOR_DELIVERY".equals(
      request.getRecommendedAction())) {
      throw new IllegalArgumentException(
        "This execution action is not supported");
    }

    try {
      DeliveryOrder order =
        deliveryOrderService.getById(request.getOrderId());

      if (!order.getOrderNumber().equals(request.getOrderNumber())) {
        throw new IllegalStateException(
          "Approval order number does not match the delivery order");
      }

      if (order.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
        throw new IllegalStateException(
          "Order is already OUT_FOR_DELIVERY; no status change is needed");
      }

      deliveryOrderService.updateStatus(
        order.getId(),
        DeliveryStatus.OUT_FOR_DELIVERY);

    } catch (RuntimeException exception) {
      auditService.recordFailure(
        id,
        exception.getMessage() == null
          ? "Execution failed"
          : exception.getMessage(),
        "demo-user");

      throw exception;
    }

    request.markExecuted("demo-user");
    return repository.save(request);
  }



}
