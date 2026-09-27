package com.service.payment.outbox;

import com.service.payment.entity.OutboxEvent;
import com.service.payment.kafka.PaymentEventProducer;
import com.service.payment.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            PaymentEventProducer paymentEventProducer
    ){
        this.outboxEventRepository = outboxEventRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents(){

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop10ByStatusOrderByCreatedAtAsc(
                                "PENDING"
                        );

        for(OutboxEvent event : events){

            System.out.println(
                    "PUBLISHING EVENT id=" + event.getId()
                    +"type=" + event.getEventType()
                    +"aggregateId=" + event.getAggregateId()
            );

            try {

                paymentEventProducer.send(
                        event.getPayload()
                );

                event.markPublished(
                        LocalDateTime.now()
                );

                outboxEventRepository.save(event);
            } catch (Exception e) {
                System.out.println(
                        "FAILED TO PUBLISH EVENT id="
                        + event.getId()
                );
            }
        }
    }
}
