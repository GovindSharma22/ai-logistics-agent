
package com.example.logisticsagent.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalRequestRepository
  extends JpaRepository<ApprovalRequest, Long> {

  List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(
    ApprovalStatus status);

  List<ApprovalRequest> findByOrderIdOrderByCreatedAtDesc(
    Long orderId);
}
