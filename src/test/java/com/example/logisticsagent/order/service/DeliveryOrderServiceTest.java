
package com.example.logisticsagent.order.service;

import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import com.example.logisticsagent.order.exception.InvalidStatusTransitionException;
import com.example.logisticsagent.order.exception.OrderNotFoundException;
import com.example.logisticsagent.order.repository.DeliveryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyCollection;
import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceTest {

  @Mock
  private DeliveryOrderRepository repository;

  @InjectMocks
  private DeliveryOrderService service;

  private DeliveryOrder order;

  @BeforeEach
  void setUp() {
    order = new DeliveryOrder(
      "ORD-TEST-001",
      "Test Customer",
      "Jaipur, Rajasthan",
      LocalDateTime.now().plusHours(3)
    );
  }

  @Test
  void shouldConfirmCreatedOrder() {
    when(repository.findById(1L)).thenReturn(Optional.of(order));
    when(repository.save(order)).thenReturn(order);

    DeliveryOrder result =
      service.updateStatus(1L, DeliveryStatus.CONFIRMED);

    assertEquals(DeliveryStatus.CONFIRMED, result.getStatus());
    verify(repository).save(order);
  }

  @Test
  void shouldRejectInvalidStatusTransition() {
    order.updateStatus(DeliveryStatus.CONFIRMED);
    when(repository.findById(1L)).thenReturn(Optional.of(order));

    assertThrows(
      InvalidStatusTransitionException.class,
      () -> service.updateStatus(1L, DeliveryStatus.DELIVERED)
    );

    verify(repository, never()).save(any(DeliveryOrder.class));
  }

  @Test
  void shouldThrowExceptionWhenOrderDoesNotExist() {
    when(repository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(
      OrderNotFoundException.class,
      () -> service.getById(999L)
    );
  }

  @Test
  void shouldSetDeliveryTimestampWhenOrderIsDelivered() {
    order.updateStatus(DeliveryStatus.CONFIRMED);
    order.updateStatus(DeliveryStatus.PICKING);
    order.updateStatus(DeliveryStatus.READY_FOR_DISPATCH);
    order.updateStatus(DeliveryStatus.OUT_FOR_DELIVERY);

    when(repository.findById(1L)).thenReturn(Optional.of(order));
    when(repository.save(order)).thenReturn(order);

    DeliveryOrder result =
      service.updateStatus(1L, DeliveryStatus.DELIVERED);

    assertEquals(DeliveryStatus.DELIVERED, result.getStatus());
    assertNotNull(result.getActualDeliveryAt());
    verify(repository).save(order);
  }

  @Test
  void shouldCalculateDeliveryMetrics() {
    when(repository.count()).thenReturn(10L);
    when(repository.countByStatus(DeliveryStatus.DELIVERED))
      .thenReturn(8L);
    when(repository.countDeliveredOnTime()).thenReturn(6L);

    when(repository.findByEstimatedDeliveryAtBeforeAndStatusNotIn(
      any(LocalDateTime.class),
      anyCollection()
    )).thenReturn(List.of(
      order
    ));

    DeliveryMetricsResponse result = service.getDeliveryMetrics();

    assertEquals(10L, result.totalOrders());
    assertEquals(8L, result.deliveredOrders());
    assertEquals(1L, result.delayedOrders());
    assertEquals(
      new BigDecimal("75.00"),
      result.onTimeDeliveryRate()
    );
  }

  @Test
  void shouldReturnZeroOnTimeRateWhenNoOrdersDelivered() {
    when(repository.count()).thenReturn(0L);
    when(repository.countByStatus(DeliveryStatus.DELIVERED))
      .thenReturn(0L);
    when(repository.countDeliveredOnTime()).thenReturn(0L);

    when(repository.findByEstimatedDeliveryAtBeforeAndStatusNotIn(
      any(LocalDateTime.class),
      anyCollection()
    )).thenReturn(List.of());

    DeliveryMetricsResponse result = service.getDeliveryMetrics();

    assertEquals(0L, result.totalOrders());
    assertEquals(0L, result.deliveredOrders());
    assertEquals(0L, result.delayedOrders());
    assertEquals(
      new BigDecimal("0.00"),
      result.onTimeDeliveryRate()
    );
  }

  @Test
  void shouldDetectHighRiskOrderWhenEtaHasPassed() {
    DeliveryOrder overdueOrder = new DeliveryOrder(
      "ORD-HIGH-001",
      "High Risk Customer",
      "Jaipur, Rajasthan",
      LocalDateTime.now().minusMinutes(10)
    );

    when(repository.findAll()).thenReturn(List.of(overdueOrder));

    List<DeliveryRiskResponse> risks = service.getDeliveryRisks();

    assertEquals(1, risks.size());
    assertEquals("ORD-HIGH-001", risks.get(0).orderNumber());
    assertEquals("HIGH", risks.get(0).riskLevel());
    assertEquals(DeliveryStatus.CREATED.name(), risks.get(0).status());
  }

  @Test
  void shouldDetectMediumRiskOrderWhenEtaIsWithin60Minutes() {
    DeliveryOrder upcomingOrder = new DeliveryOrder(
      "ORD-MEDIUM-001",
      "Medium Risk Customer",
      "Jaipur, Rajasthan",
      LocalDateTime.now().plusMinutes(30)
    );

    when(repository.findAll()).thenReturn(List.of(upcomingOrder));

    List<DeliveryRiskResponse> risks = service.getDeliveryRisks();

    assertEquals(1, risks.size());
    assertEquals("ORD-MEDIUM-001", risks.get(0).orderNumber());
    assertEquals("MEDIUM", risks.get(0).riskLevel());
    assertTrue(
      risks.get(0).reason().contains("60 minutes")
    );
  }

  @Test
  void shouldExcludeDeliveredAndCancelledOrdersFromRisks() {
    DeliveryOrder deliveredOrder = new DeliveryOrder(
      "ORD-DONE-001",
      "Delivered Customer",
      "Jaipur, Rajasthan",
      LocalDateTime.now().minusMinutes(30)
    );

    deliveredOrder.updateStatus(DeliveryStatus.CONFIRMED);
    deliveredOrder.updateStatus(DeliveryStatus.PICKING);
    deliveredOrder.updateStatus(DeliveryStatus.READY_FOR_DISPATCH);
    deliveredOrder.updateStatus(DeliveryStatus.OUT_FOR_DELIVERY);
    deliveredOrder.updateStatus(DeliveryStatus.DELIVERED);

    DeliveryOrder cancelledOrder = new DeliveryOrder(
      "ORD-CANCEL-001",
      "Cancelled Customer",
      "Jaipur, Rajasthan",
      LocalDateTime.now().minusMinutes(30)
    );

    cancelledOrder.updateStatus(DeliveryStatus.CANCELLED);

    when(repository.findAll()).thenReturn(
      List.of(deliveredOrder, cancelledOrder)
    );

    List<DeliveryRiskResponse> risks = service.getDeliveryRisks();

    assertTrue(risks.isEmpty());
  }

}
