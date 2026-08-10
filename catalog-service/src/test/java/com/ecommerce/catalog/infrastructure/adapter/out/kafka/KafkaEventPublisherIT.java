package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.annotation.KafkaListener;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=catalog-it",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.ecommerce.catalog.infrastructure.adapter.out.kafka.ProductEventMessage"
})
class KafkaEventPublisherIT {

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static final List<ProductEventMessage> received = new CopyOnWriteArrayList<>();
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));

    @Autowired
    private KafkaEventPublisher publisher;

    @KafkaListener(topics = KafkaEventPublisher.PRODUCT_EVENTS_TOPIC, groupId = "catalog-it-consumer")
    void onMessage(ProductEventMessage message) {
        received.add(message);
    }

    @Test
    void publishesProductCreatedEventWithFullState() {
        ProductId productId = ProductId.newId();
        Money price = new Money(new BigDecimal("1500.00"), Currency.getInstance("USD"));

        publisher.publish(new ProductCreatedEvent(COMPANY_ID, productId, "Notebook", price, ProductStatus.DRAFT, Instant.now()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(received).hasSize(1);
            ProductEventMessage message = received.get(0);
            assertThat(message.eventType()).isEqualTo("PRODUCT_CREATED");
            assertThat(message.companyId()).isEqualTo(COMPANY_ID.value());
            assertThat(message.productId()).isEqualTo(productId.value());
            assertThat(message.productName()).isEqualTo("Notebook");
            assertThat(message.price()).isEqualByComparingTo("1500.00");
            assertThat(message.currency()).isEqualTo("USD");
            assertThat(message.status()).isEqualTo("DRAFT");
        });
    }
}
