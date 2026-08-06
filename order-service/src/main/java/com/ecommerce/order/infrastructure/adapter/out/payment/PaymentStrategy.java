package com.ecommerce.order.infrastructure.adapter.out.payment;

import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.model.Money;

public interface PaymentStrategy {

    PaymentMethod supportedMethod();

    PaymentResult pay(Money amount);
}
