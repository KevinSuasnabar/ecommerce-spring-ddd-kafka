package com.ecommerce.order.infrastructure.adapter.out.notification;

import com.ecommerce.order.application.port.out.NotificationPort;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationAdapter.class);

    @Override
    public void orderStatusChanged(OrderId orderId, OrderStatus status, CustomerId customerId) {
        log.info("Sending notification to customer {}: order {} changed to {}",
                customerId.value(), orderId.value(), status);
    }
}
