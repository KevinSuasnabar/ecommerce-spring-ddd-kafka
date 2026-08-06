package com.ecommerce.catalog.domain.event;

import com.ecommerce.catalog.domain.model.ProductId;

import java.time.Instant;

public sealed interface DomainEvent
        permits ProductCreatedEvent, ProductUpdatedEvent, ProductPriceChangedEvent,
        ProductActivatedEvent, ProductRetiredEvent {

    ProductId productId();

    Instant occurredAt();
}
