package com.example.logisticsagent.ai.dto;

import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;

import java.util.List;

public record LogisticsInsightsResponse(
  DeliveryMetricsResponse metrics,
  String insights,
  List<DeliveryRecommendationResponse> recommendations
) {}
