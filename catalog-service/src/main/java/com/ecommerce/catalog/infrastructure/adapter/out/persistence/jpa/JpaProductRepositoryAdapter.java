package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.catalog.domain.event.DomainEvent;
import com.ecommerce.catalog.domain.event.ProductActivatedEvent;
import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.event.ProductPriceChangedEvent;
import com.ecommerce.catalog.domain.event.ProductRetiredEvent;
import com.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.ecommerce.catalog.infrastructure.adapter.out.kafka.ProductEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Profile("postgres")
public class JpaProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository jpa;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public JpaProductRepositoryAdapter(JpaProductRepository jpa,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(Product product) {
        jpa.save(toEntity(product));

        List<DomainEvent> events = product.pullDomainEvents();
        for (DomainEvent event : events) {
            ProductEventMessage message = toMessage(event);
            try {
                String payload = objectMapper.writeValueAsString(message);
                OutboxEventEntity outbox = new OutboxEventEntity(
                        UUID.randomUUID(),
                        "Product",
                        message.productId(),
                        message.eventType(),
                        payload,
                        Instant.now(),
                        false);
                outboxEventRepository.save(outbox);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize outbox event", e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(CompanyId companyId, ProductId productId) {
        return jpa.findByCompanyIdAndId(companyId.value(), productId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllByCompanyId(CompanyId companyId) {
        return jpa.findAllByCompanyId(companyId.value()).stream()
                .map(this::toDomain)
                .toList();
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

    private ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(
                product.id().value(),
                product.companyId().value(),
                product.name(),
                product.description(),
                product.price().amount(),
                product.price().currency().getCurrencyCode(),
                product.status().name(),
                product.createdAt(),
                product.updatedAt(),
                product.categories().stream()
                        .map(cat -> cat.value())
                        .collect(Collectors.toSet()));
    }

    private Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                new ProductId(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                new Money(entity.getPrice(), Currency.getInstance(entity.getCurrency())),
                new CompanyId(entity.getCompanyId()),
                ProductStatus.valueOf(entity.getStatus()),
                entity.getCategoryIds().stream()
                        .map(com.ecommerce.catalog.domain.model.CategoryId::new)
                        .collect(Collectors.toSet()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
