
package com.example.logisticsagent.alert;

import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRiskAlertScheduler {

  private static final Logger log =
    LoggerFactory.getLogger(DeliveryRiskAlertScheduler.class);

  private final DeliveryOrderService deliveryOrderService;
  private final Map<String, String> alertedRisks = new HashMap<>();

  public DeliveryRiskAlertScheduler(
    DeliveryOrderService deliveryOrderService) {
    this.deliveryOrderService = deliveryOrderService;
  }

  @Scheduled(fixedDelay = 60_000)
  public synchronized void checkDeliveryRisks() {
    List<DeliveryRiskResponse> risks =
      deliveryOrderService.getDeliveryRisks();

    Map<String, String> currentRisks = new HashMap<>();

    for (DeliveryRiskResponse risk : risks) {
      String orderNumber = risk.orderNumber();
      String riskLevel = risk.riskLevel();

      currentRisks.put(orderNumber, riskLevel);

      if (!riskLevel.equals(alertedRisks.get(orderNumber))) {
        log.warn(
          "DELIVERY RISK ALERT | Level={} | Order={} | Customer={} | Status={} | ETA={} | Reason={}",
          riskLevel,
          orderNumber,
          risk.customerName(),
          risk.status(),
          risk.estimatedDeliveryAt(),
          risk.reason()
        );
      }
    }

    alertedRisks.clear();
    alertedRisks.putAll(currentRisks);

    if (risks.isEmpty()) {
      log.info("Delivery risk check completed. No at-risk orders found.");
    }
  }
}
