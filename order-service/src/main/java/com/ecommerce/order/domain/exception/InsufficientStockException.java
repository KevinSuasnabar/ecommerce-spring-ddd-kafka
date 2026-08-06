package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.ProductId;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(OrderId orderId, ProductId productId) {
        super("Insufficient stock for product " + productId.value() + " on order " + orderId.value());
    }
}
