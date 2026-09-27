package com.service.payment.service;

import com.service.payment.entity.OutboxEvent;
import com.service.payment.entity.Payment;
import com.service.payment.exception.StaleFencingTokenException;
import com.service.payment.repository.IdempotencyFenceRepository;
import com.service.payment.repository.OutboxEventRepository;
import com.service.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentWriter {

    private final PaymentRepository paymentRepository;
    private final IdempotencyFenceRepository idempotencyFenceRepository;
    private final OutboxEventRepository outboxEventRepository;

    public PaymentWriter(
            PaymentRepository paymentRepository,
            IdempotencyFenceRepository idempotencyFenceRepository,
            OutboxEventRepository outboxEventRepository) {

        this.paymentRepository = paymentRepository;
        this.idempotencyFenceRepository = idempotencyFenceRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public Payment save(
            Payment payment,
            String idempotencyKey,
            long fencingToken) {

        Long currentToken =
                idempotencyFenceRepository.findTokenForUpdate(
                        idempotencyKey
                );

        if (currentToken == null) {
            throw new IllegalStateException(
                    "Fence record not found for key: " + idempotencyKey
            );
        }

        if (fencingToken < currentToken) {
            throw new StaleFencingTokenException(
                    "Stale fencing token. incoming="
                            + fencingToken
                            + ", current="
                            + currentToken
            );
        }

        if (fencingToken > currentToken) {
            idempotencyFenceRepository.updateToken(
                    idempotencyKey,
                    fencingToken
            );
        }

        Payment savedPayment =
                paymentRepository.saveAndFlush(payment);

        UUID eventId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                eventId,
                "PaymentCreated",
                "Payment",
                String.valueOf(savedPayment.getId()),
                "{\"eventId\":\"" + eventId
                        + "\",\"paymentId\":" + savedPayment.getId() + "}",
                "PENDING",
                LocalDateTime.now(),
                null
        );

        outboxEventRepository.save(event);

        return savedPayment;
    }
}