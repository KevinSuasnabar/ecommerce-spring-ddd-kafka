package com.ecommerce.order.domain.event;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;

import java.time.Instant;

public record OrderCancelledEvent(OrderId orderId, CompanyId companyId, String reason, Instant occurredAt) implements DomainEvent {
}
