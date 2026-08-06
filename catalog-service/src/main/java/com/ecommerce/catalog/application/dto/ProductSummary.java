package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;

public record ProductSummary(ProductId id, String name, Money price, ProductStatus status) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.name(), product.price(), product.status());
    }
}
