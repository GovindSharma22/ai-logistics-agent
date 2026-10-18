
package com.example.logisticsagent.order.exception;

public class InvalidStatusTransitionException extends RuntimeException {

  public InvalidStatusTransitionException(String message) {
    super(message);
  }
}
