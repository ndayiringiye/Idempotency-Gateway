package com.igirepay.gateway.repository;

import com.igirepay.gateway.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    // Standard CRUD utilities for MySQL are auto-generated here
}
