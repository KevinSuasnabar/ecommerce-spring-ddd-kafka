package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stock", schema = "warehouse")
@IdClass(StockJpaEntity.StockKey.class)
public class StockJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockJpaEntity() {
    }

    public StockJpaEntity(UUID companyId, UUID productId, Instant createdAt) {
        this.companyId = companyId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public UUID getCompanyId() { return companyId; }
    public UUID getProductId() { return productId; }
    public Instant getCreatedAt() { return createdAt; }

    public static class StockKey implements Serializable {
        private UUID companyId;
        private UUID productId;

        public StockKey() {}
        public StockKey(UUID companyId, UUID productId) {
            this.companyId = companyId;
            this.productId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StockKey other)) return false;
            return Objects.equals(companyId, other.companyId) && Objects.equals(productId, other.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, productId);
        }
    }
}
