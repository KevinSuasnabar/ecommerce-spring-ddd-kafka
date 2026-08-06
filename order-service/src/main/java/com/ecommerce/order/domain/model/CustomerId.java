package com.ecommerce.order.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "customer id must not be null");
    }
}
