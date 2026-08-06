package com.ecommerce.catalog.domain.exception;

import com.ecommerce.catalog.domain.model.ProductId;

public class InvalidProductTransitionException extends DomainException {

    public InvalidProductTransitionException(ProductId productId, String reason) {
        super("Invalid transition for product " + productId.value() + ": " + reason);
    }
}
