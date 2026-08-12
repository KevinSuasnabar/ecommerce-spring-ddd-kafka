package com.ecommerce.warehouse.domain.model;

import java.time.Instant;
import java.util.Objects;

public record StockMovement(StockMovementType type, Quantity quantity, Instant occurredAt) {

    public StockMovement {
        Objects.requireNonNull(type, "movement type must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
