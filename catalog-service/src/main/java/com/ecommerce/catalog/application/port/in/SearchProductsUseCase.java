package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;
import com.ecommerce.catalog.domain.model.CompanyId;

public interface SearchProductsUseCase {

    ProductPageResult search(CompanyId companyId, SearchProductsQuery query);
}
