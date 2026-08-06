package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.ProductSummary;
import com.ecommerce.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public record ProductSummaryResponse(UUID id, String name, BigDecimal price, Currency currency, ProductStatus status) {

    public static ProductSummaryResponse from(ProductSummary summary) {
        return new ProductSummaryResponse(
                summary.id().value(),
                summary.name(),
                summary.price().amount(),
                summary.price().currency(),
                summary.status());
    }
}
