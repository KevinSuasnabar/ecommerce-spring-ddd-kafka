package com.ecommerce.order.infrastructure.adapter.out.payment;

import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentMethod, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentStrategy::supportedMethod, Function.identity()));
    }

    public PaymentStrategy strategyFor(PaymentMethod method) {
        return strategies.getOrDefault(method, rejectedStrategy());
    }

    private PaymentStrategy rejectedStrategy() {
        return new PaymentStrategy() {
            @Override
            public PaymentMethod supportedMethod() {
                return null;
            }

            @Override
            public PaymentResult pay(Money amount) {
                return PaymentResult.rejected();
            }
        };
    }
}
