package com.ecommerce.order.infrastructure.adapter.out.payment;

import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentAdapter implements PaymentPort {

    private final PaymentStrategyFactory strategyFactory;

    public FakePaymentAdapter(PaymentStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Override
    public PaymentResult charge(Money amount, PaymentMethod method) {
        return strategyFactory.strategyFor(method).pay(amount);
    }
}
