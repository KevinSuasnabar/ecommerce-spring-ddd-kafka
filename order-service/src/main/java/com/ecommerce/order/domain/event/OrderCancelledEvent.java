package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.OrderId;

import java.time.Instant;

public record OrderCancelledEvent(OrderId orderId, String reason, Instant occurredAt) implements DomainEvent {
}
