package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, UUID parentId) {

    public static CategoryResponse from(CategoryQueryResult result) {
        return new CategoryResponse(
                result.id().value(),
                result.name(),
                result.parentId() == null ? null : result.parentId().value());
    }
}
