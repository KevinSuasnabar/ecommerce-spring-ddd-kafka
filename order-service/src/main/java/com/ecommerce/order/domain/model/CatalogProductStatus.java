package com.ecommerce.order.domain.model;

public enum CatalogProductStatus {

    DRAFT,
    ACTIVE,
    RETIRED;

    public static CatalogProductStatus from(String value) {
        for (CatalogProductStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown catalog product status: " + value);
    }
}
