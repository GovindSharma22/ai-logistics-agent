
package com.example.logisticsagent.approval;

import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.logisticsagent.order.entity.DeliveryStatus;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceTest {

  @Mock
  private ApprovalRequestRepository repository;

  @Mock
  private DeliveryOrderService deliveryOrderService;
  @Mock
  private ApprovalExecutionAuditService auditService;

  private ApprovalRequestService service;

  @BeforeEach
  void setUp() {
    service = new ApprovalRequestService(
      repository,
      deliveryOrderService,
      auditService
    );
  }

  @Test
  void executeApprovedAction_shouldRecordExecutionAudit()
  {
    ApprovalRequest request = new ApprovalRequest(
      3L,
      "ORD-RISK-001",
      "Move order to OUT_FOR_DELIVERY",
      "Order is ready for dispatch."
    );

    request.approve("demo-user");

    DeliveryOrder order = mock(DeliveryOrder.class);
    when(repository.findById(5L))
      .thenReturn(Optional.of(request));
    when(deliveryOrderService.getById(3L))
      .thenReturn(order);
    when(order.getId()).thenReturn(3L);
    when(order.getOrderNumber()).thenReturn("ORD-RISK-001");
    when(repository.save(request)).thenReturn(request);

    ApprovalRequest result =
      service.executeApprovedAction(5L);

    assertEquals(ApprovalStatus.EXECUTED, result.getStatus());
    assertEquals("demo-user", result.getExecutedBy());
    assertNotNull(result.getExecutedAt());
    assertNull(result.getExecutionError());

    verify(deliveryOrderService).updateStatus(
      3L,
      com.example.logisticsagent.order.entity.DeliveryStatus.OUT_FOR_DELIVERY
    );
    verify(repository).save(request);
  }
  @Test
  void executeApprovedAction_shouldRejectPendingApproval() {
    ApprovalRequest request = new ApprovalRequest(
      3L,
      "ORD-RISK-001",
      "Move order to OUT_FOR_DELIVERY",
      "Order is ready for dispatch."
    );

    when(repository.findById(5L))
      .thenReturn(Optional.of(request));

    assertThrows(
      IllegalStateException.class,
      () -> service.executeApprovedAction(5L)
    );

    verifyNoInteractions(deliveryOrderService);
    verify(repository, never()).save(any());
  }

  @Test
  void executeApprovedAction_shouldRejectUnsupportedAction() {
    ApprovalRequest request = new ApprovalRequest(
      3L,
      "ORD-RISK-001",
      "Cancel delivery",
      "Test unsupported action."
    );

    request.approve("demo-user");

    when(repository.findById(5L))
      .thenReturn(Optional.of(request));

    assertThrows(
      IllegalArgumentException.class,
      () -> service.executeApprovedAction(5L)
    );

    verifyNoInteractions(deliveryOrderService);
    verify(repository, never()).save(any());
  }

  @Test
  void executeApprovedAction_shouldNotMarkExecutedWhenOrderUpdateFails() {
    ApprovalRequest request = new ApprovalRequest(
      3L,
      "ORD-RISK-001",
      "Move order to OUT_FOR_DELIVERY",
      "Test invalid order status."
    );

    request.approve("demo-user");

    DeliveryOrder order = mock(DeliveryOrder.class);

    when(repository.findById(5L))
      .thenReturn(Optional.of(request));
    when(deliveryOrderService.getById(3L))
      .thenReturn(order);
    when(order.getId()).thenReturn(3L);
    when(order.getOrderNumber()).thenReturn("ORD-RISK-001");

    doThrow(new IllegalStateException("Invalid status transition"))
      .when(deliveryOrderService)
      .updateStatus(3L, DeliveryStatus.OUT_FOR_DELIVERY);

    assertThrows(
      IllegalStateException.class,
      () -> service.executeApprovedAction(5L)
    );

    assertEquals(ApprovalStatus.APPROVED, request.getStatus());
    assertNull(request.getExecutedAt());
    assertNull(request.getExecutedBy());

    verify(repository, never()).save(request);
  }


  @Test
  void executeApprovedAction_shouldRecordFailureWhenOrderUpdateFails() {
    ApprovalRequest request = new ApprovalRequest(
      3L,
      "ORD-RISK-001",
      "Move order to OUT_FOR_DELIVERY",
      "Test failed execution."
    );

    request.approve("demo-user");

    DeliveryOrder order = mock(DeliveryOrder.class);

    when(repository.findById(5L))
      .thenReturn(Optional.of(request));
    when(deliveryOrderService.getById(3L))
      .thenReturn(order);
    when(order.getId()).thenReturn(3L);
    when(order.getOrderNumber()).thenReturn("ORD-RISK-001");

    doThrow(new IllegalStateException("Invalid status transition"))
      .when(deliveryOrderService)
      .updateStatus(3L, DeliveryStatus.OUT_FOR_DELIVERY);

    assertThrows(
      IllegalStateException.class,
      () -> service.executeApprovedAction(5L)
    );

    verify(auditService).recordFailure(
      5L,
      "Invalid status transition",
      "demo-user"
    );

    assertEquals(ApprovalStatus.APPROVED, request.getStatus());
    verify(repository, never()).save(request);
  }

}
