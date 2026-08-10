package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.UpdateProductCommand;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.ProductId;

public interface UpdateProductUseCase {

    void updateProduct(CompanyId companyId, ProductId productId, UpdateProductCommand command);
}
