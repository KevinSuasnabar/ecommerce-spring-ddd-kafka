package com.ecommerce.catalog.domain.model;

import java.util.Map;
import java.util.Set;

public enum ProductStatus {

    DRAFT,
    ACTIVE,
    RETIRED;

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, Set.of(ACTIVE, RETIRED),
            ACTIVE, Set.of(RETIRED),
            RETIRED, Set.of()
    );

    public boolean canTransitionTo(ProductStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
