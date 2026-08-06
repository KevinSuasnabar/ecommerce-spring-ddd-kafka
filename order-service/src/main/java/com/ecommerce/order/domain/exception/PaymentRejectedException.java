package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.OrderId;

public class PaymentRejectedException extends DomainException {

    public PaymentRejectedException(OrderId orderId) {
        super("Payment rejected for order " + orderId.value());
    }
}
