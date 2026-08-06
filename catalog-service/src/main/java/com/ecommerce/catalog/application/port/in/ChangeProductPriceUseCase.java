package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.application.dto.ChangeProductPriceCommand;
import com.ecommerce.catalog.domain.model.ProductId;

public interface ChangeProductPriceUseCase {

    void changeProductPrice(ProductId productId, ChangeProductPriceCommand command);
}
