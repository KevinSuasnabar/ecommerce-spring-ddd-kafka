package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;

import java.util.List;

public interface GetCategoriesUseCase {

    List<CategoryQueryResult> getCategories();
}
