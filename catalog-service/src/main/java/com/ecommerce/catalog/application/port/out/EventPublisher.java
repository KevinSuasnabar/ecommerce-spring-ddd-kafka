package com.ecommerce.catalog.application.port.out;

import com.ecommerce.catalog.domain.event.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent event);
}
