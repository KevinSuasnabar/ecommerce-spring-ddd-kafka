package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.ProductId;

public interface ActivateProductUseCase {

    void activateProduct(CompanyId companyId, ProductId productId);
}
