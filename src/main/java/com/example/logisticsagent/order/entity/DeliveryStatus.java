package com.example.logisticsagent.order.entity;

public enum DeliveryStatus {
  CREATED,
  CONFIRMED,
  PICKING,
  READY_FOR_DISPATCH,
  OUT_FOR_DELIVERY,
  DELIVERED,
  DELAYED,
  CANCELLED
}
