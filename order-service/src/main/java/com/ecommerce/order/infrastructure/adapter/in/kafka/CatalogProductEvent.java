package com.ecommerce.order.infrastructure.adapter.in.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogProductEvent(
        String eventType,
        UUID productId,
        String productName,
        BigDecimal price,
        String currency,
        String status,
        Instant occurredAt,
        UUID companyId) {
}
