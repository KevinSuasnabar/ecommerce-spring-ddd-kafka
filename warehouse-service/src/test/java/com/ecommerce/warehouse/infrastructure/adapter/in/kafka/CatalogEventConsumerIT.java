package com.ecommerce.warehouse.infrastructure.adapter.in.kafka;

import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import com.ecommerce.warehouse.application.port.out.processedevent.ProcessedEventStore;
import com.ecommerce.warehouse.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=warehouse-it",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.ecommerce.warehouse.infrastructure.adapter.in.kafka.CatalogProductEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
class CatalogEventConsumerIT extends AbstractPostgresIT {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProcessedEventStore processedEventStore;

    @Test
    void consumingCatalogEventCreatesStockLine() {
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, productEvent(PRODUCT_ID, COMPANY));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            StockId stockId = new StockId(COMPANY, new ProductId(PRODUCT_ID));
            assertThat(stockRepository.findById(stockId)).isPresent();
        });
    }

    @Test
    void consumingEventForAnotherCompanyCreatesSeparateStockLine() {
        UUID otherProduct = UUID.fromString("40000000-0000-0000-0000-000000000009");
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, productEvent(otherProduct, OTHER_COMPANY));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            StockId otherStockId = new StockId(OTHER_COMPANY, new ProductId(otherProduct));
            assertThat(stockRepository.findById(otherStockId)).isPresent();
        });

        assertThat(stockRepository.findById(new StockId(COMPANY, new ProductId(otherProduct)))).isEmpty();
    }

    @Test
    void duplicateEventWithSameEventIdIsSkipped() {
        UUID eventId = UUID.fromString("60000000-0000-0000-0000-000000000001");
        UUID dupProductId = UUID.fromString("40000000-0000-0000-0000-000000000010");
        String event = productEvent(dupProductId, COMPANY, eventId);

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, event);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processedEventStore.isProcessed(eventId)).isTrue());

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, event);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(processedEventStore.isProcessed(eventId)).isTrue();
            assertThat(stockRepository.findById(new StockId(COMPANY, new ProductId(dupProductId)))).isPresent();
        });
    }

    private String productEvent(UUID productId, CompanyId companyId) {
        return productEvent(productId, companyId, UUID.randomUUID());
    }

    private String productEvent(UUID productId, CompanyId companyId, UUID eventId) {
        return """
                {
                  "eventType": "PRODUCT_CREATED",
                  "eventId": "%s",
                  "productId": "%s",
                  "productName": "Mouse",
                  "price": 10.00,
                  "currency": "USD",
                  "status": "ACTIVE",
                  "occurredAt": "2026-08-10T12:00:00Z",
                  "companyId": "%s"
                }
                """.formatted(eventId, productId, companyId.value());
    }
}
