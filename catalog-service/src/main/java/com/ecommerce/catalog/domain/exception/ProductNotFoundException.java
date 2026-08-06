package com.ecommerce.catalog.domain.exception;

import com.ecommerce.catalog.domain.model.ProductId;

public class ProductNotFoundException extends DomainException {

    public ProductNotFoundException(ProductId productId) {
        super("Product " + productId.value() + " not found");
    }
}
