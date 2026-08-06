package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.PaymentMethod;

public interface PaymentPort {

    PaymentResult charge(Money amount, PaymentMethod method);
}
