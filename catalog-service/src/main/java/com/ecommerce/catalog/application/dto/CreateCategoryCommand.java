package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;

public record CreateCategoryCommand(CompanyId companyId, String name, CategoryId parentId) {
}
