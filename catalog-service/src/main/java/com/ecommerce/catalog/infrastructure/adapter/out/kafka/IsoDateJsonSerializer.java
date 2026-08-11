package com.ecommerce.catalog.infrastructure.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Serializador JSON de Kafka con fechas ISO-8601 (contrato wire de
 * "catalog.products"). El JsonSerializer por defecto de Spring Boot
 * serializa Instant como epoch (1.7864088836130762E9): ambiguo.
 * La configuración de type-headers sigue viniendo de las properties
 * del yml (spring.kafka.producer.properties.*) vía configure().
 */
public class IsoDateJsonSerializer<T> extends JsonSerializer<T> {

    public IsoDateJsonSerializer() {
        super(isoDateMapper());
    }

    private static ObjectMapper isoDateMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
