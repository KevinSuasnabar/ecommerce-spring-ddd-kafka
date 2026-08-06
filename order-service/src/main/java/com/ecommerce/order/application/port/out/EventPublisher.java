package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.event.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent event);
}
