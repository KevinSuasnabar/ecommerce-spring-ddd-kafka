package com.ecommerce.warehouse.infrastructure.adapter.out.processedevent;

import com.ecommerce.warehouse.application.port.out.processedevent.ProcessedEventStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!postgres")
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<UUID> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isProcessed(UUID eventId) {
        return processed.contains(eventId);
    }

    @Override
    public void markProcessed(UUID eventId) {
        processed.add(eventId);
    }
}