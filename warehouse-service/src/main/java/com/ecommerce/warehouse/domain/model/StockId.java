package com.ecommerce.warehouse.domain.model;

import java.util.Objects;

public record StockId(CompanyId companyId, ProductId productId) {

    public StockId {
        Objects.requireNonNull(companyId, "company id must not be null");
        Objects.requireNonNull(productId, "product id must not be null");

    }
}
