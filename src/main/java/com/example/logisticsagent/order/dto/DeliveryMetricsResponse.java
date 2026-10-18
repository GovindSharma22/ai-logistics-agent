
package com.example.logisticsagent.order.dto;

import java.math.BigDecimal;

public record DeliveryMetricsResponse(
  long totalOrders,
  long deliveredOrders,
  long delayedOrders,
  BigDecimal onTimeDeliveryRate,
  long overdueUndeliveredOrders
) {}
