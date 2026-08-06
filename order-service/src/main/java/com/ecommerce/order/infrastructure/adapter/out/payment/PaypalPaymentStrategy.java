package com.ecommerce.order.infrastructure.adapter.out.payment;

import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaypalPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public PaymentResult pay(Money amount) {
        return PaymentResult.approved("PP-" + UUID.randomUUID());
    }
}
