package com.service.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_fence")
public class IdempotencyFence {

    @Id
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    protected IdempotencyFence() {
    }

    public IdempotencyFence(
            String idempotencyKey,
            long fencingToken) {

        this.idempotencyKey = idempotencyKey;
        this.fencingToken = fencingToken;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public long getFencingToken() {
        return fencingToken;
    }
}