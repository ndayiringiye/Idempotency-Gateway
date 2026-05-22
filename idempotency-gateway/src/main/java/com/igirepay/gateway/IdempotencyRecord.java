package com.igirepay.gateway.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "payload_hash", nullable = false)
    private String requestPayloadHash;

    @Column(name = "response_body", length = 1000)
    private String responseBody;

    @Column(name = "response_status_code")
    private int responseStatusCode;

    public enum Status { PROCESSING, COMPLETED }

    // Transient means it won't be saved as a table column in MySQL. 
    // It remains in-memory to handle the concurrent "In-Flight" thread locking!
    @Transient
    private final ReentrantLock lock = new ReentrantLock();
}
