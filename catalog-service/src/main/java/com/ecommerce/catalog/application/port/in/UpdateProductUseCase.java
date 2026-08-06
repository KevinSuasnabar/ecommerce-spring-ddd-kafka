package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.UpdateProductCommand;
import com.ecommerce.catalog.domain.model.ProductId;

public interface UpdateProductUseCase {

    void updateProduct(ProductId productId, UpdateProductCommand command);
}
