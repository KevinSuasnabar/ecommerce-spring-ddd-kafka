package com.ecommerce.order.infrastructure.adapter.in.kafka;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
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

import java.time.Duration;
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

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CatalogProductStore store;

    @Test
    void consumesCatalogProductEventAndUpsertsSnapshot() {
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, productEvent("PRODUCT_CREATED", "Keyboard", "75.00", "ACTIVE"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().productName()).isEqualTo("Keyboard");
            assertThat(product.get().price().amount()).isEqualByComparingTo("75.00");
            assertThat(product.get().price().currency()).isEqualTo(Currency.getInstance("USD"));
            assertThat(product.get().status()).isEqualTo(CatalogProductStatus.ACTIVE);
            assertThat(product.get().canBeOrdered()).isTrue();
        });
    }

    @Test
    void retiredEventMakesSnapshotNotOrderable() {
        kafkaTemplate.send(CatalogEventConsumer.CATALOG_PRODUCTS_TOPIC, productEvent("PRODUCT_RETIRED", "Keyboard", "75.00", "RETIRED"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Optional<CatalogProduct> product = store.findById(new ProductId(PRODUCT_ID));
            assertThat(product).isPresent();
            assertThat(product.get().status()).isEqualTo(CatalogProductStatus.RETIRED);
            assertThat(product.get().canBeOrdered()).isFalse();
        });
    }

    private String productEvent(String eventType, String name, String price, String status) {
        return """
                {
                  "eventType": "%s",
                  "productId": "%s",
                  "productName": "%s",
                  "price": %s,
                  "currency": "USD",
                  "status": "%s",
                  "occurredAt": "2026-08-04T12:00:00Z"
                }
                """.formatted(eventType, PRODUCT_ID, name, price, status);
    }
}
