package com.igirepay.gateway.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igirepay.gateway.exception.IdempotencyException;
import com.igirepay.gateway.model.*;
import com.igirepay.gateway.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final PaymentProcessor paymentProcessor;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.processing-delay:2000}")
    private long processingDelay;

    /**
     * Main method to process payment with Idempotency logic
     */
    public ResponseEntity<PaymentResponse> processPayment(String idempotencyKey, PaymentRequest request) {

        // Use your model's hash method
        String requestHash = request.generatePayloadHash();

        Optional<IdempotencyRecord> existingOpt = repository.findByIdempotencyKey(idempotencyKey);

        if (existingOpt.isPresent()) {
            IdempotencyRecord record = existingOpt.get();

            // Different Request Body with Same Key → 409 Conflict
            if (!record.getRequestPayloadHash().equals(requestHash)) {
                throw new IdempotencyException("Idempotency key already used for a different request body.", 409);
            }

            // Bonus User Story: In-Flight Request Handling
            if (record.getStatus() == IdempotencyRecord.Status.PROCESSING) {
                return waitForInFlightRequest(record);
            }

            // Cache Hit - Return saved response
            PaymentResponse response = parseResponse(record.getResponseBody());
            return ResponseEntity.status(record.getResponseStatusCode())
                    .header("X-Cache-Hit", "true")
                    .body(response);
        }

        // === First Time Request ===
        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setIdempotencyKey(idempotencyKey);
        newRecord.setRequestPayloadHash(requestHash);
        newRecord.setStatus(IdempotencyRecord.Status.PROCESSING);
        repository.save(newRecord);

        try {
            // Process payment with simulated delay
            PaymentResponse response = paymentProcessor.processPayment(request, processingDelay);

            // Update record as completed
            newRecord.setStatus(IdempotencyRecord.Status.COMPLETED);
            newRecord.setResponseBody(objectMapper.writeValueAsString(response));
            newRecord.setResponseStatusCode(200);
            repository.save(newRecord);

            return ResponseEntity.ok()
                    .header("X-Cache-Hit", "false")
                    .body(response);

        } catch (Exception e) {
            newRecord.setStatus(IdempotencyRecord.Status.FAILED);
            repository.save(newRecord);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    /**
     * Bonus: Wait for in-flight request to complete (Prevents race conditions)
     */
    private ResponseEntity<PaymentResponse> waitForInFlightRequest(IdempotencyRecord record) {
        int maxAttempts = 300; // Maximum 30 seconds (100ms intervals)

        for (int i = 0; i < maxAttempts; i++) {
            Optional<IdempotencyRecord> updatedOpt = repository.findByIdempotencyKey(record.getIdempotencyKey());

            if (updatedOpt.isPresent()) {
                IdempotencyRecord updated = updatedOpt.get();

                if (updated.getStatus() != IdempotencyRecord.Status.PROCESSING) {
                    PaymentResponse response = parseResponse(updated.getResponseBody());
                    return ResponseEntity.status(updated.getResponseStatusCode())
                            .header("X-Cache-Hit", "true")
                            .body(response);
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IdempotencyException("Request interrupted while waiting", 500);
            }
        }

        throw new IdempotencyException("Payment processing timeout", 504);
    }

    /**
     * Helper method to convert JSON string back to PaymentResponse
     */
    private PaymentResponse parseResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse cached response", e);
        }
    }
}