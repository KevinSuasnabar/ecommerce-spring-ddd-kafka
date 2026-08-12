package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Profile("postgres")
public interface JpaStockMovementRepository extends JpaRepository<StockMovementJpaEntity, UUID> {

    List<StockMovementJpaEntity> findAllByCompanyIdAndProductId(UUID companyId, UUID productId);
}
