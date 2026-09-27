package com.service.payment.controller;

import com.service.payment.consumer.PaymentEventConsumer;
import com.service.payment.entity.Payment;
import com.service.payment.kafka.PaymentEventProducer;
import com.service.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentEventConsumer paymentEventConsumer;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentController(
            PaymentService paymentService,
            PaymentEventConsumer paymentEventConsumer,
            PaymentEventProducer paymentEventProducer
            ){

        this.paymentService = paymentService;
        this.paymentEventConsumer = paymentEventConsumer;
        this.paymentEventProducer = paymentEventProducer;
    }

    @PostMapping
    public Payment createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Payment payment){
        return paymentService.createPayment(idempotencyKey ,payment);
    }

    @PostMapping("/test-event/{eventId}")
    public void testEvent(
            @PathVariable UUID eventId
            ){
        paymentEventConsumer.consume(eventId);
    }

    @PostMapping("/test-kafka")
    public void testkafka(){
        paymentEventProducer.send(
                "hello from springboot"
        );
    }


}
