package com.ecommerce.warehouse.infrastructure.adapter.in.kafka;

import com.ecommerce.warehouse.application.port.in.EnsureStockExistsUseCase;
import com.ecommerce.warehouse.application.port.out.processedevent.ProcessedEventStore;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventConsumer {

    public static final String CATALOG_PRODUCTS_TOPIC = "catalog.products";

    private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);

    private final EnsureStockExistsUseCase ensureStockExistsUseCase;
    private final ProcessedEventStore processedEventStore;

    public CatalogEventConsumer(EnsureStockExistsUseCase ensureStockExistsUseCase,
                                 ProcessedEventStore processedEventStore) {
        this.ensureStockExistsUseCase = ensureStockExistsUseCase;
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = CATALOG_PRODUCTS_TOPIC)
    public void onCatalogProductEvent(CatalogProductEvent event) {
        if (processedEventStore.isProcessed(event.eventId())) {
            log.info("Skipping duplicate catalog event {} (eventId={})", event.eventType(), event.eventId());
            return;
        }

        CompanyId companyId = new CompanyId(event.companyId());
        ProductId productId = new ProductId(event.productId());
        ensureStockExistsUseCase.ensureStockExists(companyId, productId);
        processedEventStore.markProcessed(event.eventId());
        log.info("Synced stock line for company {} product {} ({}) (eventId={})",
                companyId, productId, event.eventType(), event.eventId());
    }
}