package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;

public record CategoryQueryResult(CategoryId id, String name, CategoryId parentId) {

    public static CategoryQueryResult from(Category category) {
        return new CategoryQueryResult(category.id(), category.name(), category.parentId());
    }
}
