package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderStatus;

public interface NotificationPort {

    void orderStatusChanged(OrderId orderId, OrderStatus status, CustomerId customerId);
}
