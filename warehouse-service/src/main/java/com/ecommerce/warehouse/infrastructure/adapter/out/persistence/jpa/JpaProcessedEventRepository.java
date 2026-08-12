package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@Profile("postgres")
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}