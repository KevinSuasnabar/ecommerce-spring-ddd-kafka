package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.ProductId;

public interface RemoveCategoryFromProductUseCase {

    void removeCategory(ProductId productId, CategoryId categoryId);
}
