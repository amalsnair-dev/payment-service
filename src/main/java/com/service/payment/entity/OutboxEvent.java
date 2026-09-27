package com.service.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private UUID id;

    private String eventType;

    @Column(name = "aggregated_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    private String payload;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    protected OutboxEvent(){
    }

    public OutboxEvent(

            UUID id,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            String status,
            LocalDateTime createdAt,
            LocalDateTime publishedAt
    ){

        this.id = id;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId(){
        return id;
    }

    public String getEventType(){
        return eventType;
    }

    public String getAggregateType(){
        return aggregateType;
    }

    public String getAggregateId(){
        return aggregateId;
    }

    public String getStatus(){
        return status;
    }

    public String getPayload(){
        return payload;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getPublishedAt(){
        return publishedAt;
    }

    public void markPublished(LocalDateTime publishedAt){
        this.status = "PUBLISHED";
        this.publishedAt = publishedAt;
    }

}
