package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.RoundRobinPartitioner;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demuestra en vivo el particionado de Kafka con su propia key:
 * (1) la misma key SIEMPRE cae en la misma partición → orden garantizado por entidad;
 * (2) sin key, el particionador por defecto es STICKY (KIP-480): junta mensajes en una
 *     partición para batch eficiente;
 * (3) con RoundRobinPartitioner explícito, sin key, los mensajes se reparten.
 */
@Testcontainers(disabledWithoutDocker = true)
class KafkaPartitioningIT {

    static final String TOPIC = "partitioning.demo";
    static final int PARTITIONS = 3;

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Test
    void sameKeyAlwaysLandsOnSamePartitionAndNoKeyBehavesPerPartitioner() throws Exception {
        String bootstrapServers = kafka.getBootstrapServers();

        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) 1)))
                    .all().get(10, TimeUnit.SECONDS);
        }

        List<Integer> sameKeyPartitions = send(bootstrapServers, "c-1001:p-1", "EVENTO-", null);
        List<Integer> defaultStickyPartitions = send(bootstrapServers, null, "sin-key-", null);
        List<Integer> roundRobinPartitions = send(bootstrapServers, null, "round-robin-", RoundRobinPartitioner.class.getName());

        printReport(sameKeyPartitions, defaultStickyPartitions, roundRobinPartitions);

        assertThat(sameKeyPartitions).hasSize(6);
        assertThat(sameKeyPartitions.stream().distinct()).containsExactly(sameKeyPartitions.get(0));
        assertThat(roundRobinPartitions).hasSize(6);
        assertThat(roundRobinPartitions.stream().distinct().count()).isGreaterThan(1);
    }

    private List<Integer> send(String bootstrapServers, String key, String valuePrefix, String partitionerClass) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        if (partitionerClass != null) {
            props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, partitionerClass);
        }

        List<Integer> partitions = new ArrayList<>();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 1; i <= 6; i++) {
                RecordMetadata md = producer.send(new ProducerRecord<>(TOPIC, key, valuePrefix + i))
                        .get(10, TimeUnit.SECONDS);
                partitions.add(md.partition());
            }
        }
        return partitions;
    }

    private void printReport(List<Integer> sameKey, List<Integer> sticky, List<Integer> roundRobin) {
        System.out.println("=== MISMA KEY 'c-1001:p-1' (6 eventos) ===");
        System.out.println("    partición de cada evento: " + sameKey);
        System.out.println("    => siempre la partición " + sameKey.get(0) + ". Orden garantizado.");
        System.out.println("=== SIN KEY, particionador por defecto (sticky, KIP-480) ===");
        System.out.println("    partición de cada mensaje: " + sticky);
        System.out.println("    => tiende a juntarse en una partición para batch eficiente.");
        System.out.println("=== SIN KEY, RoundRobinPartitioner explícito ===");
        System.out.println("    partición de cada mensaje: " + roundRobin);
        System.out.println("    => se reparten en " + roundRobin.stream().distinct().count() + " particiones.");
    }
}
