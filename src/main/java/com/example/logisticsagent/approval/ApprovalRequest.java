
package com.example.logisticsagent.approval;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long orderId;

  @Column(nullable = false, length = 100)
  private String orderNumber;

  @Column(nullable = false, length = 1000)
  private String recommendedAction;

  @Column(nullable = false, length = 2000)
  private String explanation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApprovalStatus status = ApprovalStatus.PENDING;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime decidedAt;

  @Column(length = 100)
  private String decidedBy;

  @Column(length = 100)
  private String executedBy;

  private LocalDateTime executedAt;

  @Column(length = 1000)
  private String executionError;

  protected ApprovalRequest() {
  }

  public ApprovalRequest(
    Long orderId,
    String orderNumber,
    String recommendedAction,
    String explanation) {
    this.orderId = orderId;
    this.orderNumber = orderNumber;
    this.recommendedAction = recommendedAction;
    this.explanation = explanation;
    this.status = ApprovalStatus.PENDING;
    this.createdAt = LocalDateTime.now();
  }

  public void approve(String username) {
    ensurePending();
    this.status = ApprovalStatus.APPROVED;
    this.decidedBy = username;
    this.decidedAt = LocalDateTime.now();
  }

  public void reject(String username) {
    ensurePending();
    this.status = ApprovalStatus.REJECTED;
    this.decidedBy = username;
    this.decidedAt = LocalDateTime.now();
  }

  public void markExecuted(String username) {
    if (this.status != ApprovalStatus.APPROVED) {
      throw new IllegalStateException(
        "Only approved requests can be executed");
    }

    this.status = ApprovalStatus.EXECUTED;
    this.executedBy = username;
    this.executedAt = LocalDateTime.now();
    this.executionError = null;
  }

  public void markFailed(String error, String username) {
    if (this.status != ApprovalStatus.APPROVED) {
      throw new IllegalStateException(
        "Only approved requests can be marked failed");
    }

    this.status = ApprovalStatus.FAILED;
    this.executedBy = username;
    this.executedAt = LocalDateTime.now();
    this.executionError = error;
  }

  private void ensurePending() {
    if (this.status != ApprovalStatus.PENDING) {
      throw new IllegalStateException(
        "Only pending requests can be approved or rejected");
    }
  }

  public Long getId() {
    return id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }

  public String getExplanation() {
    return explanation;
  }

  public ApprovalStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getDecidedAt() {
    return decidedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public String getExecutedBy() {
    return executedBy;
  }

  public LocalDateTime getExecutedAt() {
    return executedAt;
  }

  public String getExecutionError() {
    return executionError;
  }
}
