package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.ProductStatus;

public record SearchProductsQuery(String keyword, CategoryId categoryId, ProductStatus status, int page, int size) {

    public SearchProductsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
