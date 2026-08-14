package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa.OutboxEventEntity;
import com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, ProductEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        KafkaTemplate<String, ProductEventMessage> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEventEntity> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.info("OutboxPoller found {} pending events", pending.size());
        for (OutboxEventEntity entity : pending) {
            try {
                ProductEventMessage message = objectMapper.readValue(entity.getPayload(), ProductEventMessage.class);
                String key = message.companyId() + ":" + message.productId();
                kafkaTemplate.send(ProductEventMessage.PRODUCT_EVENTS_TOPIC, key, message)
                        .get(5, TimeUnit.SECONDS);
                entity.markPublished();
                outboxEventRepository.save(entity);
                log.info("Published outbox event {} ({}) for key {}", entity.getId(), entity.getEventType(), key);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", entity.getId(), e.getMessage());
            }
        }
    }
}
