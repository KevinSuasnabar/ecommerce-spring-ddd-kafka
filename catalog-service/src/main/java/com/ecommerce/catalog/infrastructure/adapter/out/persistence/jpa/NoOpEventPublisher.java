package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.catalog.application.port.out.EventPublisher;
import com.ecommerce.catalog.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.debug("Domain event {} handled by outbox adapter, skipping direct publish", event.getClass().getSimpleName());
    }
}
