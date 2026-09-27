package com.service.payment.idempotency;

public record LeaseOwnership(
        String ownerToken,
        Long fencingToken
) {
}
