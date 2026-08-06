package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.OrderId;

import java.time.Instant;

public record OrderConfirmedEvent(OrderId orderId, Instant occurredAt) implements DomainEvent {
}
