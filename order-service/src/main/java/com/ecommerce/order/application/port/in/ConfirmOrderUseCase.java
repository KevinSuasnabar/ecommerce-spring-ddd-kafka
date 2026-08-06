package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.OrderId;

public interface ConfirmOrderUseCase {

    void confirmOrder(OrderId orderId);
}
