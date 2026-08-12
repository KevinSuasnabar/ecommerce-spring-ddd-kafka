package com.ecommerce.order.infrastructure.adapter.in.kafka;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.application.port.out.processedevent.ProcessedEventStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=order-it",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.ecommerce.order.infrastructure.adapter.in.kafka.CatalogProductEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
class CatalogEventConsumerIT {

    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
    private static final Currency USD = Currency.getInstance("USD");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CatalogProductStore store;

    @Autowired
    private ProcessedEventStore processedEventStore;

    @Test
    void consumesCatalogProductEventAndUpsertsSnapshot() {
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC,
                productEvent("PRODUCT_CREATED", "Keyboard", "75.00", "ACTIVE", COMPANY));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(COMPANY, new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().companyId()).isEqualTo(COMPANY);
            assertThat(product.get().productName()).isEqualTo("Keyboard");
            assertThat(product.get().price().amount()).isEqualByComparingTo("75.00");
            assertThat(product.get().price().currency()).isEqualTo(Currency.getInstance("USD"));
            assertThat(product.get().status()).isEqualTo(CatalogProductStatus.ACTIVE);
            assertThat(product.get().canBeOrdered()).isTrue();
        });
    }

    @Test
    void retiredEventMakesSnapshotNotOrderable() {
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC,
                productEvent("PRODUCT_RETIRED", "Keyboard", "75.00", "RETIRED", COMPANY));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(COMPANY, new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().status()).isEqualTo(CatalogProductStatus.RETIRED);
            assertThat(product.get().canBeOrdered()).isFalse();
        });
    }

    @Test
    void productUpdatedEventRefreshesSnapshot(){
        CatalogProduct old = new CatalogProduct(COMPANY, new ProductId(PRODUCT_ID), "Old name",
                new Money(new BigDecimal("75.00"), USD), CatalogProductStatus.ACTIVE, Instant.now());
        store.upsert(old);   // 1. Estado VIEJO en el snapshot

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC,
                productEvent("PRODUCT_UPDATED", "Keyboard Pro", "75.00", "ACTIVE", COMPANY));   // 2. llega evento con estado NUEVO

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(COMPANY, new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().productName()).isEqualTo("Keyboard Pro");   // 3. el NUEVO ganó (refresh real)
        });
    }

    @Test
    void productPriceChangedEventUpdatesPrice(){
        CatalogProduct old = new CatalogProduct(COMPANY, new ProductId(PRODUCT_ID), "Old name",
                new Money(new BigDecimal("75.00"), USD), CatalogProductStatus.ACTIVE, Instant.now());
        store.upsert(old);

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC,
                productEvent("PRODUCT_PRICE_CHANGED", "Keyboard Pro", "90.00", "ACTIVE", COMPANY));


        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(COMPANY, new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().price().amount()).isEqualByComparingTo("90.00");   // 3. el NUEVO ganó (refresh real)
        });
    }

    @Test
    void eventFromAnotherCompanyDoesNotLeakIntoOwnSnapshot() {
        UUID otherProductId = UUID.fromString("30000000-0000-0000-0000-000000000009");
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC,
                productEvent("PRODUCT_CREATED", "Keyboard de otro tenant", "999.00", "ACTIVE", OTHER_COMPANY, otherProductId));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(store.findById(OTHER_COMPANY, new ProductId(otherProductId))).isPresent());

        assertThat(store.findById(COMPANY, new ProductId(otherProductId))).isEmpty();
    }

    @Test
    void duplicateEventWithSameEventIdIsSkipped() {
        UUID eventId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        UUID dupProductId = UUID.fromString("30000000-0000-0000-0000-000000000010");
        String event = productEvent("PRODUCT_CREATED", "Mouse Gamer", "25.00", "ACTIVE",
                COMPANY, dupProductId, eventId);

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, event);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processedEventStore.isProcessed(eventId)).isTrue());

        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, event);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(processedEventStore.isProcessed(eventId)).isTrue();
            assertThat(store.findById(COMPANY, new ProductId(dupProductId))).isPresent();
        });
    }

    private String productEvent(String eventType, String name, String price, String status, CompanyId companyId) {
        return productEvent(eventType, name, price, status, companyId, PRODUCT_ID);
    }

    private String productEvent(String eventType, String name, String price, String status, CompanyId companyId, UUID productId) {
        return productEvent(eventType, name, price, status, companyId, productId, UUID.randomUUID());
    }

    private String productEvent(String eventType, String name, String price, String status,
                                CompanyId companyId, UUID productId, UUID eventId) {
        return """
                {
                  "eventType": "%s",
                  "eventId": "%s",
                  "productId": "%s",
                  "productName": "%s",
                  "price": %s,
                  "currency": "USD",
                  "status": "%s",
                  "occurredAt": "2026-08-04T12:00:00Z",
                  "companyId": "%s"
                }
                """.formatted(eventType, eventId, productId, name, price, status, companyId.value());
    }
}
