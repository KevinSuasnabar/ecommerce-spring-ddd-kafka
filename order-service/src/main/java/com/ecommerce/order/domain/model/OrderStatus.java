package com.ecommerce.order.domain.model;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
