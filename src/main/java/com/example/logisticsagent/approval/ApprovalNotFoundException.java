package com.example.logisticsagent.approval;

public class ApprovalNotFoundException extends RuntimeException {

  public ApprovalNotFoundException(Long id) {
    super("Approval request not found: " + id);
  }
}
