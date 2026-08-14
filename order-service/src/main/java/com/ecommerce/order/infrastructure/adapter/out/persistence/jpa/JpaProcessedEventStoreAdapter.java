package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.order.application.port.out.processedevent.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaProcessedEventStoreAdapter implements ProcessedEventStore {

    private final JpaProcessedEventRepository jpa;

    public JpaProcessedEventStoreAdapter(JpaProcessedEventRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean isProcessed(UUID eventId) {
        return jpa.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId) {
        jpa.save(new ProcessedEventEntity(eventId, Instant.now()));
    }
}