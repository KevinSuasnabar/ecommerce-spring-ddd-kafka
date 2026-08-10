package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(@NotBlank String name, UUID parentId) {

    public CreateCategoryCommand toCommand(CompanyId companyId) {
        return new CreateCategoryCommand(companyId, name, parentId == null ? null : new CategoryId(parentId));
    }
}
