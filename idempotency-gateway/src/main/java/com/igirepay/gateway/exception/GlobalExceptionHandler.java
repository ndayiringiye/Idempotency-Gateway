package com.igirepay.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom idempotency exceptions (409, 400, 504, etc.)
     */
    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<Object> handleIdempotencyException(IdempotencyException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "error", ex.getMessage(),
                        "status", ex.getStatusCode()
                ));
    }

    /**
     * Handle validation errors (e.g., @Valid annotations)
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Validation failed", "details", ex.getMessage()));
    }

    /**
     * Catch-all for unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "An unexpected error occurred",
                        "message", ex.getMessage()
                ));
    }
}