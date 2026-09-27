package com.service.payment.consumer;

import com.service.payment.entity.ProcessedEvent;
import com.service.payment.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ){
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "payment.created",
            groupId = "payment-service"
    )

    @Transactional
    public void receive(
            String message,
            Acknowledgment acknowledgment
    ){
        try{
            JsonNode json =
                    objectMapper.readTree(message);

            UUID eventId =
                    UUID.fromString(
                            json.get("eventId").asText()
                    );

            Long paymentId =
                    json.get("paymentId").asLong();

            System.out.println(
                    "KAFKA EVEN RECEIVED"
                    + "eventId=" + eventId
                    +"paymentId" + paymentId
            );
            consume(eventId);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to process kafka event:"

                    + message,
                    e
            );
        }
    }

    @Transactional
    public void consume(UUID eventId){

        int inserted =
                processedEventRepository.claimEvent(eventId);

        if(inserted == 0){

            System.out.println(
                    "DUPLICATE EVENT - ignoring eventId="
                    + eventId
            );

            return;
        }

        System.out.println(
                "EVENT CLAIMED eventId="
                + eventId
        );
//        throw new IllegalStateException(
//                "TEST: consumer failed after claiming event"
//        );

        System.out.println(
                "EVENT PROCESSED eventId="
                + eventId
        );

    }
}
