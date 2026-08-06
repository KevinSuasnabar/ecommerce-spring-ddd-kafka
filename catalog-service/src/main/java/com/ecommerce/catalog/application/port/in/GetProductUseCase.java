package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.ProductQueryResult;
import com.ecommerce.catalog.domain.model.ProductId;

public interface GetProductUseCase {

    ProductQueryResult getProduct(ProductId productId);
}
