package com.ecommerce.warehouse.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CompanyId(UUID value) {

    public CompanyId {
        Objects.requireNonNull(value, "Company id must not be null");
    }
}
