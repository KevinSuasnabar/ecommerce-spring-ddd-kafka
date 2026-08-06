package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.ProductId;

public interface InventoryPort {

    boolean reserve(ProductId productId, int quantity);
}
