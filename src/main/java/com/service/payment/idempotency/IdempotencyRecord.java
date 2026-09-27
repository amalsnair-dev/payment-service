package com.service.payment.idempotency;

import com.service.payment.entity.Payment;

import java.util.concurrent.CompletableFuture;

public class IdempotencyRecord {

    private final IdempotencyStatus status;
    private final Payment payment;
    private final CompletableFuture<Payment> future;

    public IdempotencyRecord(
            IdempotencyStatus status,
            Payment payment
            ){

        this.status = status;
        this.payment = payment;
        this.future = new CompletableFuture<>();
    }

    public IdempotencyRecord(
            IdempotencyStatus status,
            Payment payment,
            CompletableFuture<Payment> future
            ){
        this.status = status;
        this.payment = payment;
        this.future = future;
    }

    public IdempotencyStatus getStatus(){
        return status;
    }

    public Payment getPayment(){
        return payment;
    }

     public CompletableFuture<Payment> getFuture(){
        return future;
     }
}
