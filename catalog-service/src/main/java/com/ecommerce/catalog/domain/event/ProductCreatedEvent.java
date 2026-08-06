package com.ecommerce.catalog.domain.event;

import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;

import java.time.Instant;

public record ProductCreatedEvent(ProductId productId, String productName, Money price, ProductStatus status, Instant occurredAt)
        implements DomainEvent {
}
