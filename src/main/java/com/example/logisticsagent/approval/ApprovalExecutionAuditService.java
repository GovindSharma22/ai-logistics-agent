
package com.example.logisticsagent.approval;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalExecutionAuditService {

  private final ApprovalRequestRepository repository;

  public ApprovalExecutionAuditService(
    ApprovalRequestRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(Long id, String error, String username) {
    ApprovalRequest request = repository.findById(id)
      .orElseThrow(() -> new ApprovalNotFoundException(id));

    if (request.getStatus() != ApprovalStatus.APPROVED) {
      return;
    }

    request.markFailed(error, username);
    repository.save(request);
  }
}
