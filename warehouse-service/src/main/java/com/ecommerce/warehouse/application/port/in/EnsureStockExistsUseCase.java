package com.ecommerce.warehouse.application.port.in;

import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;

public interface EnsureStockExistsUseCase {

    void ensureStockExists(CompanyId companyId, ProductId productId);
}
