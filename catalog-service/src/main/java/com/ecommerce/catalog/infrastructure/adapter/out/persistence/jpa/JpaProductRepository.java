package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("postgres")
public interface JpaProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByCompanyIdAndId(UUID companyId, UUID id);

    List<ProductJpaEntity> findAllByCompanyId(UUID companyId);
}
