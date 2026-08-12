package com.ecommerce.order.application.port.out.processedevent;

import java.util.UUID;

public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId);

    void markProcessed(UUID eventId);
}