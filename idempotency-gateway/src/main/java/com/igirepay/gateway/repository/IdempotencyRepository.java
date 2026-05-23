package com.igirepay.gateway.repository;

import com.igirepay.gateway.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Find record by Idempotency-Key (This is the most important query)
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}