package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import com.ecommerce.catalog.AbstractPostgresIT;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Prueba de CONTRATO (consumer-driven): verifica que el JSON que catalog
 * publica en "catalog.products" contiene EXACTAMENTE los campos que
 * order-service espera deserializar en CatalogProductEvent.
 * <p>
 * Si en catalog se renombra o elimina un campo (ej. productName -> name),
 * este test falla y avisa del quiebre de contrato antes de que order
 * consuma datos corruptos en producción. Sin acoplar los dos micros.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.kafka.consumer.group-id=catalog-contract-it",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
})
class CatalogProductContractIT extends AbstractPostgresIT {

    /**
     * El contrato: campos que order-service (CatalogProductEvent) espera recibir.
     */
    static final List<String> ORDER_CONTRACT_FIELDS = List.of(
            "eventType", "eventId", "productId", "productName", "price", "currency", "status", "occurredAt", "companyId");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static final CopyOnWriteArrayList<RawMessage> rawMessages = new CopyOnWriteArrayList<>();

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = ProductEventMessage.PRODUCT_EVENTS_TOPIC, groupId = "catalog-contract-consumer")
    void onRawMessage(ConsumerRecord<String, String> record) {
        rawMessages.add(new RawMessage(record.key(), record.value()));
    }

    @Test
    void publishedJsonMatchesOrderContract() throws Exception {
        CompanyId companyId = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
        Money price = new Money(new BigDecimal("1500.00"), Currency.getInstance("USD"));
        ProductId productId = ProductId.newId();

        Product product = Product.create(productId, "Notebook", null, price, companyId);
        productRepository.save(product);
        outboxPoller.pollAndPublish();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(rawMessages).hasSize(1));

        RawMessage message = rawMessages.get(0);

        assertThat(message.key().split(":"))
                .containsExactly(companyId.value().toString(), productId.value().toString());

        JsonNode json = objectMapper.readTree(message.value());

        assertThat(actualFieldNames(json)).containsAll(ORDER_CONTRACT_FIELDS);

        for (String field : ORDER_CONTRACT_FIELDS) {
            assertThat(json.get(field).asText()).as("field %s must not be null", field).isNotBlank();
        }

        assertThat(UUID.fromString(json.get("productId").asText())).isNotNull();
        assertThat(json.get("price").isNumber()).isTrue();
        assertThat(Instant.parse(json.get("occurredAt").asText())).isNotNull();
    }

    private List<String> actualFieldNames(JsonNode json) {
        List<String> names = new ArrayList<>();
        json.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private record RawMessage(String key, String value) {
    }
}
