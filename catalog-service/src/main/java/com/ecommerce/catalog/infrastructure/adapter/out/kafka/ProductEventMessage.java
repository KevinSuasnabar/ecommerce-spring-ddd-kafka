package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductEventMessage(
        String eventType,
        UUID eventId,
        UUID productId,
        String productName,
        BigDecimal price,
        String currency,
        String status,
        Instant occurredAt,
        UUID companyId) {
}