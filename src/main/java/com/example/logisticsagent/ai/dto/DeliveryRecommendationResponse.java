package com.example.logisticsagent.ai.dto;

public record DeliveryRecommendationResponse(
  String orderNumber,
  String riskLevel,
  String reason,
  String recommendedAction,
  String explanation
) {}
