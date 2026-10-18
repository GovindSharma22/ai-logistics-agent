package com.example.logisticsagent.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
            )
        );
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateOrder(
            DuplicateOrderException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage()
            )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", message
            )
        );
    }



  @ExceptionHandler(InvalidStatusTransitionException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(
    InvalidStatusTransitionException ex) {

    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      Map.of(
        "timestamp", Instant.now().toString(),
        "status", 409,
        "error", "Conflict",
        "message", ex.getMessage()
      )
    );
  }


  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
    IllegalArgumentException ex) {

    return ResponseEntity.badRequest().body(
      Map.of(
        "timestamp", Instant.now().toString(),
        "status", 400,
        "error", "Bad Request",
        "message", ex.getMessage()
      )
    );
  }

}
