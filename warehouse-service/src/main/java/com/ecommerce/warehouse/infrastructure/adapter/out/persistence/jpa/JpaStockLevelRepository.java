package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@Profile("postgres")
public interface JpaStockLevelRepository extends JpaRepository<StockLevelEntity, StockLevelEntity.StockLevelKey> {

    Optional<StockLevelEntity> findByCompanyIdAndProductId(UUID companyId, UUID productId);
}