package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCatalogProductSnapshotRepository
        extends JpaRepository<CatalogProductSnapshotEntity, CatalogProductSnapshotKey> {

    Optional<CatalogProductSnapshotEntity> findByCompanyIdAndProductId(java.util.UUID companyId, java.util.UUID productId);
}
