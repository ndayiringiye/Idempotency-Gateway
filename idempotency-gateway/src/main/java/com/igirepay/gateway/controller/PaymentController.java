package com.igirepay.gateway.services;

import com.igirepay.gateway.lock.IdempotencyLockManager;
import com.igirepay.gateway.model.PaymentRequest;
import com.igirepay.gateway.model.PaymentResponse;
import com.igirepay.gateway.repository.IdempotencyRepository;
import com.igirepay.gateway.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;

    // ===============================
    // IN-FLIGHT TRACKING (NEW)
    // ===============================
    private static final ConcurrentHashMap<String, CountDownLatch> inFlightMap = new ConcurrentHashMap<>();

    public ResponseEntity<PaymentResponse> processPayment(
            String key,
            PaymentRequest request
    ) {

        ReentrantLock lock = IdempotencyLockManager.getLock(key);

        lock.lock(); // ensures in-flight protection

        try {

            // ===============================
            // IN-FLIGHT CHECK (NEW)
            // ===============================
            if (inFlightMap.containsKey(key)) {
                try {
                    inFlightMap.get(key).await(); // WAIT for Request A
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                var cached = repository.findByIdempotencyKey(key);

                if (cached.isPresent()) {
                    return ResponseEntity
                            .ok()
                            .header("X-Cache-Hit", "true")
                            .body(cached.get().getResponse());
                }
            }

            // mark as IN-FLIGHT
            CountDownLatch latch = new CountDownLatch(1);
            inFlightMap.put(key, latch);

            String requestHash = HashUtil.hashRequest(request);

            var existing = repository.findByIdempotencyKey(key);

            // ===============================
            // 1. EXISTING REQUEST
            // ===============================
            if (existing.isPresent()) {

                var record = existing.get();

                // SAME REQUEST → return cached response
                if (record.getRequestHash().equals(requestHash)) {

                    latch.countDown();
                    inFlightMap.remove(key);

                    return ResponseEntity
                            .ok()
                            .header("X-Cache-Hit", "true")
                            .body(record.getResponse());
                }

                // DIFFERENT REQUEST → reject (fraud protection)
                latch.countDown();
                inFlightMap.remove(key);

                return ResponseEntity
                        .status(409)
                        .body(buildFailureResponse(request,
                                "Idempotency key already used for a different request body"));
            }

            // ===============================
            // 2. FIRST REQUEST (PROCESS PAYMENT)
            // ===============================
            simulateProcessing();

            PaymentResponse response = buildSuccessResponse(request);

            repository.save(key, requestHash, response);

            latch.countDown(); // release waiting threads
            inFlightMap.remove(key);

            return ResponseEntity
                    .ok()
                    .header("X-Cache-Hit", "false")
                    .body(response);

        } finally {
            lock.unlock(); // ALWAYS RELEASE LOCK SAFELY
        }
    }

    // ===============================
    // Helpers (UNCHANGED)
    // ===============================

    private void simulateProcessing() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private PaymentResponse buildSuccessResponse(PaymentRequest request) {
        return new PaymentResponse(
                "TX-" + System.currentTimeMillis(),
                "SUCCESS",
                "Charged " + request.getAmount() + " " + request.getCurrency(),
                request.getAmount(),
                request.getCurrency(),
                LocalDateTime.now()
        );
    }

    private PaymentResponse buildFailureResponse(PaymentRequest request, String message) {
        return new PaymentResponse(
                null,
                "FAILED",
                message,
                request.getAmount(),
                request.getCurrency(),
                null
        );
    }
}