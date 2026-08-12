package com.ecommerce.order.infrastructure.adapter.in.kafka;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.application.port.out.processedevent.ProcessedEventStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.CompanyId;
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
    private final ProcessedEventStore processedEventStore;

    public CatalogEventConsumer(CatalogProductStore store, ProcessedEventStore processedEventStore) {
        this.store = store;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = CATALOG_PRODUCTS_TOPIC)
    public void onCatalogProductEvent(CatalogProductEvent event) {
        if (processedEventStore.isProcessed(event.eventId())) {
            log.info("Skipping duplicate catalog event {} (eventId={})", event.eventType(), event.eventId());
            return;
        }

        CatalogProduct product = new CatalogProduct(
                new CompanyId(event.companyId()),
                new ProductId(event.productId()),
                event.productName(),
                new Money(event.price(), Currency.getInstance(event.currency())),
                CatalogProductStatus.from(event.status()),
                event.occurredAt());
        store.upsert(product);
        processedEventStore.markProcessed(event.eventId());
        log.info("Upserted product snapshot {} for company {} ({}) from event {} (eventId={})",
                product.productId(), product.companyId(), product.productName(), event.eventType(), event.eventId());
    }
}