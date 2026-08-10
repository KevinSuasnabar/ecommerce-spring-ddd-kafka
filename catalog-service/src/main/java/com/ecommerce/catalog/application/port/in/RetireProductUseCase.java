package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.ProductId;

public interface RetireProductUseCase {

    void retireProduct(CompanyId companyId, ProductId productId);
}
