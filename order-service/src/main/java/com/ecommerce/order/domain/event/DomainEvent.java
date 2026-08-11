package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;

import java.time.Instant;

public sealed interface DomainEvent
        permits OrderCreatedEvent, OrderConfirmedEvent, OrderShippedEvent, OrderDeliveredEvent, OrderCancelledEvent {

    OrderId orderId();

    CompanyId companyId();

    Instant occurredAt();
}
