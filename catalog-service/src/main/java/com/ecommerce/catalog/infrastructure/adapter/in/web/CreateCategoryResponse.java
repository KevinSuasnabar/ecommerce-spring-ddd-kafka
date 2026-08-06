package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.domain.model.CategoryId;

import java.util.UUID;

public record CreateCategoryResponse(UUID categoryId) {

    public static CreateCategoryResponse from(CategoryId categoryId) {
        return new CreateCategoryResponse(categoryId.value());
    }
}
