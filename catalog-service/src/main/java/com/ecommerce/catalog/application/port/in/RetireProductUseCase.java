package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.ProductId;

public interface RetireProductUseCase {

    void retireProduct(ProductId productId);
}
