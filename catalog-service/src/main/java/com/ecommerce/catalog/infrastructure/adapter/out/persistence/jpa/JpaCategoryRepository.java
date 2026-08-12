package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Profile("postgres")
public interface JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    List<CategoryJpaEntity> findAllByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndName(UUID companyId, String name);
}
