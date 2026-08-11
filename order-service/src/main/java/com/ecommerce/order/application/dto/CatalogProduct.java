package com.ecommerce.order.application.dto;

import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;

import java.time.Instant;
import java.util.Objects;

public record CatalogProduct(
        CompanyId companyId,
        ProductId productId,
        String productName,
        Money price,
        CatalogProductStatus status,
        Instant updatedAt) {

    public CatalogProduct {
        Objects.requireNonNull(companyId, "companyId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public boolean canBeOrdered() {
        return status == CatalogProductStatus.ACTIVE;
    }
}
