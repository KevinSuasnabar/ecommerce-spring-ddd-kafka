package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.CategoryId;

public record CreateCategoryCommand(String name, CategoryId parentId) {
}
