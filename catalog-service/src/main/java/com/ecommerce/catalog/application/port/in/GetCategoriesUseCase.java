package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;
import com.ecommerce.catalog.domain.model.CompanyId;

import java.util.List;

public interface GetCategoriesUseCase {

    List<CategoryQueryResult> getCategories(CompanyId companyId);
}
