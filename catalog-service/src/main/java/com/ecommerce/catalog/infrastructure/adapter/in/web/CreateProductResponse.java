package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.domain.model.ProductId;

import java.util.UUID;

public record CreateProductResponse(UUID productId) {

    public static CreateProductResponse from(ProductId productId) {
        return new CreateProductResponse(productId.value());
    }
}
