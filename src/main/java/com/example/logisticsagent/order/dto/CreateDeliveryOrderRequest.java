package com.example.logisticsagent.order.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateDeliveryOrderRequest(

  @NotBlank
  @Size(max = 50)
  String orderNumber,

  @NotBlank
  @Size(max = 100)
  String customerName,

  @NotBlank
  @Size(max = 500)
  String deliveryAddress,

  @NotNull
  @Future
  LocalDateTime estimatedDeliveryAt

) {}
