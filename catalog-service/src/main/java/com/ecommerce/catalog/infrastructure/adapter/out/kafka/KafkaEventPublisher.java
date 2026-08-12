package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import com.ecommerce.catalog.application.port.out.EventPublisher;
import com.ecommerce.catalog.domain.event.DomainEvent;
import com.ecommerce.catalog.domain.event.ProductActivatedEvent;
import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.event.ProductPriceChangedEvent;
import com.ecommerce.catalog.domain.event.ProductRetiredEvent;
import com.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Profile("!postgres")
@Component
public class KafkaEventPublisher implements EventPublisher {

    public static final String PRODUCT_EVENTS_TOPIC = "catalog.products";

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, ProductEventMessage> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, ProductEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        ProductEventMessage message = toMessage(event);
        kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, message.companyId() + ":" + message.productId(), message);
        log.info("Published {} to {} for company {} product {}", message.eventType(), PRODUCT_EVENTS_TOPIC, message.companyId(), message.productId());
    }

    private ProductEventMessage toMessage(DomainEvent event) {
        if (event instanceof ProductCreatedEvent e) {
            return message("PRODUCT_CREATED", e.productId(), e.productName(), e.price(), e.status(), e.occurredAt(), e.companyId());
        }
        if (event instanceof ProductUpdatedEvent e) {
            return message("PRODUCT_UPDATED", e.productId(), e.productName(), e.price(), e.status(), e.occurredAt(), e.companyId());
        }
        if (event instanceof ProductPriceChangedEvent e) {
            return message("PRODUCT_PRICE_CHANGED", e.productId(), e.productName(), e.newPrice(), e.status(), e.occurredAt(), e.companyId());
        }
        if (event instanceof ProductActivatedEvent e) {
            return message("PRODUCT_ACTIVATED", e.productId(), e.productName(), e.price(), e.status(), e.occurredAt(), e.companyId());
        }
        if (event instanceof ProductRetiredEvent e) {
            return message("PRODUCT_RETIRED", e.productId(), e.productName(), e.price(), e.status(), e.occurredAt(), e.companyId());
        }
        throw new IllegalArgumentException("Unknown domain event: " + event.getClass().getName());
    }

    private ProductEventMessage message(String eventType, ProductId productId, String productName,
                                        Money price, ProductStatus status, Instant occurredAt, CompanyId companyId) {
        return new ProductEventMessage(
                eventType,
                UUID.randomUUID(),
                productId.value(),
                productName,
                price.amount(),
                price.currency().getCurrencyCode(),
                status.name(),
                occurredAt,
                companyId.value());
    }
}
