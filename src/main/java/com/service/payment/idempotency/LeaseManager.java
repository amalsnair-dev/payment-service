package com.service.payment.idempotency;

public interface LeaseManager {

    void start(String key, String ownerToken);
    void stop(String key, String ownerToken);
}
