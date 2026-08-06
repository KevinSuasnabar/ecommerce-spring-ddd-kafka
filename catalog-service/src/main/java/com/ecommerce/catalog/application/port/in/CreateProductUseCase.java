package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.CreateProductCommand;
import com.ecommerce.catalog.domain.model.ProductId;

public interface CreateProductUseCase {

    ProductId createProduct(CreateProductCommand command);
}
