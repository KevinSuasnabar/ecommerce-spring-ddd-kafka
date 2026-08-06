package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.OrderId;

public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(OrderId orderId) {
        super("Order " + orderId.value() + " not found");
    }
}
