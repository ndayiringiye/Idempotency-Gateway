package com.igirepay.gateway.model;

import lombok.Data;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class IdempotencyRecord {
    public enum Status { PROCESSING, COMPLETED }

    private Status status;
    private String requestPayloadHash; // Secure SHA-256 fingerprint token string
    private String responseBody;
    private int responseStatusCode;
    
    // Explicitly blocks identical concurrent requests (Bonus Story)
    private final ReentrantLock lock = new ReentrantLock();
}
