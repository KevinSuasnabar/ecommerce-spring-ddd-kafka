package com.ecommerce.order.infrastructure.adapter.out.event;

import com.ecommerce.order.application.port.out.EventPublisher;
import com.ecommerce.order.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
        log.info("Published domain event: {}", event.getClass().getSimpleName());
    }
}
