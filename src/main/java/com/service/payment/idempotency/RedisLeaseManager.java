package com.service.payment.idempotency;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLeaseManager implements LeaseManager {

    private final IdempotencyStore idempotencyStore;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    private final Map<String, LeaseEntry> activeLeases =
            new ConcurrentHashMap<>();

    private record LeaseEntry(
            String ownerToken,
            ScheduledFuture<?> future
    ) {
    }

    public RedisLeaseManager(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public void start(String key, String ownerToken) {

        ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> {

                            boolean renewed =
                                    idempotencyStore.renew(
                                            key,
                                            ownerToken
                                    );

                            System.out.println(
                                    "HEARTBEAT key=" + key
                                            + " renewed=" + renewed
                            );

                            if (!renewed) {

                                LeaseEntry current =
                                        activeLeases.get(key);

                                if (current != null &&
                                        current.ownerToken().equals(ownerToken)) {

                                    activeLeases.remove(
                                            key,
                                            current
                                    );

                                    current.future().cancel(false);
                                }

                                System.out.println(
                                        "Lease lost for key: " + key
                                );
                            }

                        },
                        20,
                        60,
                        TimeUnit.SECONDS
                );

        LeaseEntry previous =
                activeLeases.put(
                        key,
                        new LeaseEntry(
                                ownerToken,
                                future
                        )
                );

        if (previous != null) {
            previous.future().cancel(false);
        }
    }

    @Override
    public void stop(String key, String ownerToken) {

        LeaseEntry entry =
                activeLeases.get(key);

        if (entry != null &&
                entry.ownerToken().equals(ownerToken)) {

            activeLeases.remove(key, entry);

            entry.future().cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}