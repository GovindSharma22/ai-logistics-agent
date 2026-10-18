
package com.example.logisticsagent.order.dto;

import java.time.LocalDateTime;

public record DeliveryRiskResponse(
  Long orderId,
  String orderNumber,
  String customerName,
  String status,
  LocalDateTime estimatedDeliveryAt,
  String riskLevel,
  String reason
) {
}
