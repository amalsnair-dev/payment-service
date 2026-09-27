package com.service.payment.repository;

import com.service.payment.entity.IdempotencyFence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IdempotencyFenceRepository
        extends JpaRepository<IdempotencyFence, String> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO idempotency_fence (
                idempotency_key,
                fencing_token
            )
            VALUES (
                :key,
                :token
            )
            ON CONFLICT (idempotency_key)
            DO UPDATE SET
                fencing_token = EXCLUDED.fencing_token
            WHERE idempotency_fence.fencing_token < EXCLUDED.fencing_token
            """, nativeQuery = true)
    int updateIfNewer(
            @Param("key") String key,
            @Param("token") long token
    );

    @Modifying
    @Query("""
    UPDATE IdempotencyFence f
    SET f.fencingToken = :token
    WHERE f.idempotencyKey = :key
      AND f.fencingToken < :token
    """)
    int updateToken(
            @Param("key") String key,
            @Param("token") long token
    );

    @Query(value = """
        SELECT fencing_token
        FROM idempotency_fence
        WHERE idempotency_key = :key
        FOR UPDATE
        """, nativeQuery = true)
    Long findTokenForUpdate(
            @Param("key") String key
    );
}