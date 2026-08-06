package com.ecommerce.order.domain.model;

import java.util.Objects;

public record OrderLine(ProductId productId, String productName, int quantity, Money unitPrice) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId must not be null");
        requireNonBlank(productName, "productName");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
