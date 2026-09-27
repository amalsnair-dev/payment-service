package com.service.payment.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisIdempotencyStore implements IdempotencyStore{

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LeaseOwnership tryStart(String key) {

        String ownerToken = UUID.randomUUID().toString();

        String script = """
            if redis.call('exists', KEYS[1]) == 0 then

                local fencingToken =
                    redis.call('incr', KEYS[2])

                redis.call(
                    'set',
                    KEYS[1],
                    'PROCESSING|' .. ARGV[1] .. '|' .. fencingToken,
                    'EX',
                    60
                )

                return fencingToken
            end

            return 0
            """;

        DefaultRedisScript<Long> redisScript =
                new DefaultRedisScript<>(script, Long.class);

        Long fencingToken =
                redisTemplate.execute(
                        redisScript,
                        java.util.List.of(
                                key,
                                "idempotency:fencing:counter"
                        ),
                        ownerToken
                );

        if (fencingToken == null || fencingToken == 0) {
            return null;
        }

        return new LeaseOwnership(
                ownerToken,
                fencingToken
        );
    }

    @Override
    public IdempotencyStatus getStatus(String key){

        String value = redisTemplate.opsForValue()
                .get(key);

        if (value == null) {
            return null;
        }

        String status = value.split("\\|")[0];

        return IdempotencyStatus.valueOf(status);
    }

    @Override
    public boolean renew(String key, String ownerToken) {

        String script = """
            local value = redis.call('get', KEYS[1])

            if value == false then
                return 0
            end

            local expectedPrefix = 'PROCESSING|' .. ARGV[1] .. '|'

            if string.sub(value, 1, string.len(expectedPrefix))
                    == expectedPrefix then

                redis.call('expire', KEYS[1], 60)
                return 1
            end

            return 0
            """;

        DefaultRedisScript<Long> redisScript =
                new DefaultRedisScript<>(script, Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                java.util.Collections.singletonList(key),
                ownerToken
        );

        return Long.valueOf(1).equals(result);
    }

    @Override
    public void markCompleted(String key, String ownerToken) {

        String script = """
            local value = redis.call('get', KEYS[1])

            if value == false then
                return 0
            end

            local expectedPrefix = 'PROCESSING|' .. ARGV[1] .. '|'

            if string.sub(value, 1, string.len(expectedPrefix))
                    == expectedPrefix then

                redis.call(
                    'set',
                    KEYS[1],
                    'COMPLETED',
                    'EX',
                    60
                )

                return 1
            end

            return 0
            """;

        DefaultRedisScript<Long> redisScript =
                new DefaultRedisScript<>(script, Long.class);

        redisTemplate.execute(
                redisScript,
                java.util.Collections.singletonList(key),
                ownerToken
        );
    }
}