package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.ProductQueryResult;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Currency currency,
        ProductStatus status,
        List<UUID> categories,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(ProductQueryResult result) {
        return new ProductResponse(
                result.id().value(),
                result.name(),
                result.description(),
                result.price().amount(),
                result.price().currency(),
                result.status(),
                result.categories().stream().map(categoryId -> categoryId.value()).toList(),
                result.createdAt(),
                result.updatedAt());
    }
}
