package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.domain.model.CategoryId;

public interface CreateCategoryUseCase {

    CategoryId createCategory(CreateCategoryCommand command);
}
