package com.ecommerce.order.infrastructure.adapter.in.kafka;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class CatalogEventConsumer {

    public static final String CATALOG_PRODUCTS_TOPIC = "catalog.products";

    private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);

    private final CatalogProductStore store;

    public CatalogEventConsumer(CatalogProductStore store) {
        this.store = store;
    }

    @KafkaListener(topics = CATALOG_PRODUCTS_TOPIC)
    public void onCatalogProductEvent(CatalogProductEvent event) {
        CatalogProduct product = new CatalogProduct(
                new ProductId(event.productId()),
                event.productName(),
                new Money(event.price(), Currency.getInstance(event.currency())),
                CatalogProductStatus.from(event.status()),
                event.occurredAt());
        store.upsert(product);
        log.info("Upserted product snapshot {} ({}) from event {}", product.productId(), product.productName(), event.eventType());
    }
}
