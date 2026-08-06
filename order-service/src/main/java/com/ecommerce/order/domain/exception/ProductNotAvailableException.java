package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.ProductId;

public class ProductNotAvailableException extends DomainException {

    public ProductNotAvailableException(ProductId productId) {
        super("Product is not available for ordering: " + productId.value());
    }
}
