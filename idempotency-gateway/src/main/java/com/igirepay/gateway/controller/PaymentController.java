package com.igirepay.gateway.controller;

import com.igirepay.gateway.exception.IdempotencyException;
import com.igirepay.gateway.model.PaymentRequest;
import com.igirepay.gateway.model.PaymentResponse;
import com.igirepay.gateway.services.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")   // Changed to /api for better REST practice
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;

    /**
     * Main Endpoint - Process Payment with Full Idempotency Support
     */
    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        if (idempotencyKey == null || idempotencyKey.trim().isBlank()) {
            throw new IdempotencyException("Idempotency-Key header is required", 400);
        }

        return idempotencyService.processPayment(idempotencyKey, request);
    }
}