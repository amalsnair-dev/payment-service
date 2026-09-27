package com.service.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    private LocalDateTime processedAt;

    protected ProcessedEvent(){
    }

    public ProcessedEvent(
            UUID eventId,
            LocalDateTime processedAt
    ){
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    @Override
    public UUID getId() {
        return eventId;
    }

    @Override
    public boolean isNew(){
        return true;
    }

    public UUID getEventid(){
        return eventId;
    }

    public LocalDateTime getProcessedAt(){
        return processedAt;
    }

}
