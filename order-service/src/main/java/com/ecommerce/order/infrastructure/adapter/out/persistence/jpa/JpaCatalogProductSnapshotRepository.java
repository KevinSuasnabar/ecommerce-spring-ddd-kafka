package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Profile("postgres")
public interface JpaCatalogProductSnapshotRepository
        extends JpaRepository<CatalogProductSnapshotEntity, CatalogProductSnapshotKey> {

    Optional<CatalogProductSnapshotEntity> findByCompanyIdAndProductId(java.util.UUID companyId, java.util.UUID productId);
}
