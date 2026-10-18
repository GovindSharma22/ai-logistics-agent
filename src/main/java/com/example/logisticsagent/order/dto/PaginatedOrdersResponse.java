
package com.example.logisticsagent.order.dto;

import com.example.logisticsagent.order.entity.DeliveryOrder;
import java.util.List;

public record PaginatedOrdersResponse(
  List<DeliveryOrder> orders,
  int page,
  int size,
  long totalElements,
  int totalPages,
  boolean first,
  boolean last
) {}
