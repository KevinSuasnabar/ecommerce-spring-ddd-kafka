package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.OrderId;

public interface DeliverOrderUseCase {

    void deliverOrder(OrderId orderId);
}
