package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;

public interface SearchProductsUseCase {

    ProductPageResult search(SearchProductsQuery query);
}
