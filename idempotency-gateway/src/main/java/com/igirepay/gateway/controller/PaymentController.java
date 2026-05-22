package com.igirepay.gateway.controller;

import com.igirepay.gateway.model.IdempotencyRecord;
import com.igirepay.gateway.model.PaymentRequest;
import com.igirepay.gateway.repository.IdempotencyRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/process-payment")
public class PaymentController {

    private final IdempotencyRepository repository;
    
    // In-memory registry to keep track of individual transaction locks
    private final Map<String, ReentrantLock> lockRegistry = new ConcurrentHashMap<>();

    // Constructor injection for the XAMPP Database Repository
    public PaymentController(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<String> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest requestBody) {

        // Early check: validation fails if key header is missing
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Idempotency-Key header.");
        }

        // Developer's Choice Innovation: Generate cryptographic SHA-256 fingerprint
        String currentPayloadHash = requestBody.generatePayloadHash();

        // Obtain or initialize an in-memory thread lock specifically for this unique key
        ReentrantLock executionLock = lockRegistry.computeIfAbsent(idempotencyKey, k -> new ReentrantLock());

        // Lock execution. Parallel duplicate transactions will block here safely (Bonus Story)
        executionLock.lock();
        try {
            // Check if the record already exists in the XAMPP MySQL database
            Optional<IdempotencyRecord> existingRecordOpt = repository.findById(idempotencyKey);

            if (existingRecordOpt.isPresent()) {
                IdempotencyRecord record = existingRecordOpt.get();

                // User Story 3: Fraud check validation via SHA-256 fingerprint mismatch
                if (!record.getRequestPayloadHash().equals(currentPayloadHash)) {
                    throw new IllegalArgumentException("Idempotency key already used for a different request body.");
                }

                // User Story 2: Instant cached response delivery with custom replay indicator header
                if (record.getStatus() == IdempotencyRecord.Status.COMPLETED) {
                    return ResponseEntity.status(record.getResponseStatusCode())
                            .header("X-Cache-Hit", "true")
                            .body(record.getResponseBody());
                }
            }

            // User Story 1: Save immediate initial state to XAMPP database (Status: PROCESSING)
            IdempotencyRecord newRecord = new IdempotencyRecord();
            newRecord.setIdempotencyKey(idempotencyKey);
            newRecord.setStatus(IdempotencyRecord.Status.PROCESSING);
            newRecord.setRequestPayloadHash(currentPayloadHash);
            repository.save(newRecord);

            // User Story 1: Simulate internal payment gateway processor lag (2-second delay)
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Formulate standard success processing confirmation message
            String successMessage = String.format("Charged %d %s", requestBody.getAmount(), requestBody.getCurrency());

            // Commit final completion data parameters directly back into XAMPP MySQL
            newRecord.setResponseBody(successMessage);
            newRecord.setResponseStatusCode(200);
            newRecord.setStatus(IdempotencyRecord.Status.COMPLETED);
            repository.save(newRecord);

            return ResponseEntity.ok(successMessage);

        } finally {
            // Always free the lock execution path
            executionLock.unlock();
            
            // Clean up the memory register map once processing is completed to prevent memory exhaustion
            lockRegistry.remove(idempotencyKey);
        }
    }
}
