
package com.example.logisticsagent.approval;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/approvals")
public class ApprovalRequestController {

  private final ApprovalRequestService service;

  public ApprovalRequestController(ApprovalRequestService service) {
    this.service = service;
  }

  @GetMapping("/pending")
  public List<ApprovalRequest> getPendingApprovals() {
    return service.getPendingApprovals();
  }

  @GetMapping("/orders/{orderId}")
  public List<ApprovalRequest> getOrderHistory(
    @PathVariable Long orderId) {
    return service.getOrderHistory(orderId);
  }

  @PostMapping("/{id}/approve")
  public ApprovalRequest approve(@PathVariable Long id) {
    return service.approve(id, "demo-user");
  }

  @PostMapping("/{id}/reject")
  public ApprovalRequest reject(@PathVariable Long id) {
    return service.reject(id, "demo-user");
  }




  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApprovalRequest createApproval(
    @RequestBody @Valid CreateApprovalRequest request) {
    return service.create(
      request.orderId(),
      request.orderNumber(),
      request.recommendedAction(),
      request.explanation()
    );
  }





  @ExceptionHandler(ApprovalNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleNotFound(ApprovalNotFoundException exception) {
    return exception.getMessage();
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public String handleInvalidDecision(IllegalStateException exception) {
    return exception.getMessage();
  }
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleInvalidAction(IllegalArgumentException exception) {
    return exception.getMessage();
  }
  @PostMapping("/{id}/execute")
  public ApprovalRequest execute(@PathVariable Long id) {
    return service.executeApprovedAction(id);
  }
}

