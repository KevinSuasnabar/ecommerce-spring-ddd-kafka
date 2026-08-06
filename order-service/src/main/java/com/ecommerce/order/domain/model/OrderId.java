package com.ecommerce.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "order id must not be null");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
