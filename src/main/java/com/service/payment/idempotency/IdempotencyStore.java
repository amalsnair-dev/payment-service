package com.service.payment.idempotency;

public interface IdempotencyStore {

    LeaseOwnership tryStart(String key);

    IdempotencyStatus getStatus(String key);

    boolean renew(String key, String ownerToken);

    void markCompleted(String key, String ownerToken);


}