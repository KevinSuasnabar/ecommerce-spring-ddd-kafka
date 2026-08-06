package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;

import java.time.Instant;
import java.util.Set;

public record ProductQueryResult(
        ProductId id,
        String name,
        String description,
        Money price,
        ProductStatus status,
        Set<CategoryId> categories,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductQueryResult from(Product product) {
        return new ProductQueryResult(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.status(),
                product.categories(),
                product.createdAt(),
                product.updatedAt());
    }
}
