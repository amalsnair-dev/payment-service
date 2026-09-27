package com.service.payment.service;

import com.service.payment.entity.Payment;
import com.service.payment.entity.PaymentStatus;
import com.service.payment.exception.StaleFencingTokenException;
import com.service.payment.idempotency.IdempotencyStatus;
import com.service.payment.idempotency.IdempotencyStore;
import com.service.payment.idempotency.LeaseManager;
import com.service.payment.idempotency.LeaseOwnership;
import com.service.payment.repository.IdempotencyFenceRepository;
import com.service.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyFenceRepository idempotencyFenceRepository;
    private final IdempotencyStore idempotencyStore;
    private final PaymentWriter paymentWriter;
    private final LeaseManager leaseManager;

    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyFenceRepository idempotencyFenceRepository,
            IdempotencyStore idempotencyStore,
            PaymentWriter paymentWriter,
            LeaseManager leaseManager){
        this.paymentRepository = paymentRepository;
        this.idempotencyFenceRepository = idempotencyFenceRepository;
        this.idempotencyStore = idempotencyStore;
        this.paymentWriter = paymentWriter;
        this.leaseManager = leaseManager;
    }

    public Payment createPayment(
            String idempotencyKey,
            Payment payment) {

        LeaseOwnership ownership = idempotencyStore.tryStart(idempotencyKey);

        if(ownership != null){
            idempotencyFenceRepository.updateIfNewer(
                    idempotencyKey,
                    ownership.fencingToken()
            );


        }

        String ownerToken =
                ownership != null
                        ? ownership.ownerToken()
                        : null;

        Long fencingToken =
                ownership != null
                ? ownership.fencingToken()
                        :0L;

        if (ownerToken == null) {

            long timeout = System.currentTimeMillis() + 5000;

            while(System.currentTimeMillis() < timeout){

                IdempotencyStatus status =
                            idempotencyStore.getStatus(idempotencyKey);

                if (status == null) {

                    ownership =
                            idempotencyStore.tryStart(idempotencyKey);

                    if (ownership != null) {

                        ownerToken =
                                ownership.ownerToken();

                        fencingToken =
                                ownership.fencingToken();

                        idempotencyFenceRepository.updateIfNewer(
                                idempotencyKey,
                                fencingToken
                        );


                        break;
                    }
                }

                if(status == IdempotencyStatus.COMPLETED) {

                    return paymentRepository
                            .findByIdempotencyKey(
                                    idempotencyKey
                            )
                            .orElseThrow(()->
                                    new IllegalStateException(
                                            "Payment marked completed but not found"
                                    )
                                    );
                }

                Payment existingPayment =
                        paymentRepository
                                .findByIdempotencyKey(idempotencyKey)
                                .orElse(null);

                if(existingPayment != null){
                    return existingPayment;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Waiting for the payment completion was interrupted",
                            e
                    );
                }
            }

            if(ownerToken == null) {

                throw new IllegalStateException(
                        "Payment is already being processed"
                );

            }
        }

        leaseManager.start(
                idempotencyKey,
                ownerToken
        );


        try {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCreatedAt(LocalDateTime.now());

            payment.setIdempotencyKey(idempotencyKey);

            if ("fence-renew-test".equals(idempotencyKey)) {                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Payment processing was interrupted",
                            e
                    );
                }
            }

            Payment savedPayment;

            try {
                savedPayment = paymentWriter.save(
                        payment,
                        idempotencyKey,
                        fencingToken);

            } catch (DataIntegrityViolationException e) {
                Payment existingPayment = paymentRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> e);

                idempotencyStore.markCompleted(
                        idempotencyKey,
                        ownerToken
                );

                return existingPayment;
            }



            idempotencyStore.markCompleted(
                    idempotencyKey,
                    ownerToken);

            return savedPayment;

        } finally {

            leaseManager.stop(
                    idempotencyKey,
                    ownerToken
            );
        }
    }
}
