package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;

import java.time.Instant;

public record OrderShippedEvent(OrderId orderId, CompanyId companyId, Instant occurredAt) implements DomainEvent {
}
