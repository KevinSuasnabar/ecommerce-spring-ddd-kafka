package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@Profile("postgres")
public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByCompanyIdAndId(UUID companyId, UUID id);
}
