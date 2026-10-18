
package com.example.logisticsagent.alert;

import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRiskAlertSchedulerTest {

    @Mock
    private DeliveryOrderService deliveryOrderService;

    private DeliveryRiskAlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DeliveryRiskAlertScheduler(deliveryOrderService);
    }

    private DeliveryRiskResponse risk(String level) {
        return new DeliveryRiskResponse(
            1L,
            "ORD-TEST-001",
            "Test Customer",
            "OUT_FOR_DELIVERY",
            LocalDateTime.now().plusMinutes(30),
            level,
            "Test risk reason"
        );
    }

    @Test
    void shouldFetchRisksWhenScheduledCheckRuns() {
        when(deliveryOrderService.getDeliveryRisks())
            .thenReturn(List.of(risk("HIGH")));

        scheduler.checkDeliveryRisks();

        verify(deliveryOrderService, times(1)).getDeliveryRisks();
    }

    @Test
    void shouldHandleNoAtRiskOrders() {
        when(deliveryOrderService.getDeliveryRisks())
            .thenReturn(List.of());

        scheduler.checkDeliveryRisks();

        verify(deliveryOrderService, times(1)).getDeliveryRisks();
    }

    @Test
    void shouldCheckRisksAgainOnSubsequentRuns() {
        when(deliveryOrderService.getDeliveryRisks())
            .thenReturn(List.of(risk("HIGH")));

        scheduler.checkDeliveryRisks();
        scheduler.checkDeliveryRisks();

        verify(deliveryOrderService, times(2)).getDeliveryRisks();
    }

    @Test
    void shouldHandleRiskLevelChange() {
        when(deliveryOrderService.getDeliveryRisks())
            .thenReturn(List.of(risk("MEDIUM")))
            .thenReturn(List.of(risk("HIGH")));

        scheduler.checkDeliveryRisks();
        scheduler.checkDeliveryRisks();

        verify(deliveryOrderService, times(2)).getDeliveryRisks();
    }
}

