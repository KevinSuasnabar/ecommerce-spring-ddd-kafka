package com.ecommerce.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {

    public CategoryId {
        Objects.requireNonNull(value, "category id must not be null");
    }

    public static CategoryId newId() {
        return new CategoryId(UUID.randomUUID());
    }
}
