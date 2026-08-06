package com.ecommerce.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "product id must not be null");
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }
}
