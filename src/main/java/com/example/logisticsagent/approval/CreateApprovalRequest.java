
package com.example.logisticsagent.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import jakarta.validation.Valid;

public record CreateApprovalRequest(
  @NotNull Long orderId,
  @NotBlank @Size(max = 100) String orderNumber,
  @NotBlank @Size(max = 1000) String recommendedAction,
  @NotBlank @Size(max = 2000) String explanation
) {
}
