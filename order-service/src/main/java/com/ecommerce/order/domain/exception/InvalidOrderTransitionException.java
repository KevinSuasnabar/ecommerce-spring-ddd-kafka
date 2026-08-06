package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderStatus;

public class InvalidOrderTransitionException extends DomainException {

    public InvalidOrderTransitionException(OrderId orderId, OrderStatus current, OrderStatus target) {
        super("Order " + orderId.value() + " cannot transition from " + current + " to " + target);
    }
}
