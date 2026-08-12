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
@Table(name = "stock_level", schema = "warehouse")
@IdClass(StockLevelEntity.StockLevelKey.class)
public class StockLevelEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockLevelEntity() {
    }

    public StockLevelEntity(UUID companyId, UUID productId, int available, int reserved, Instant updatedAt) {
        this.companyId = companyId;
        this.productId = productId;
        this.available = available;
        this.reserved = reserved;
        this.updatedAt = updatedAt;
    }

    public UUID getCompanyId() { return companyId; }
    public UUID getProductId() { return productId; }
    public int getAvailable() { return available; }
    public int getReserved() { return reserved; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAvailable(int available) { this.available = available; }
    public void setReserved(int reserved) { this.reserved = reserved; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class StockLevelKey implements Serializable {
        private UUID companyId;
        private UUID productId;

        public StockLevelKey() {}

        public StockLevelKey(UUID companyId, UUID productId) {
            this.companyId = companyId;
            this.productId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StockLevelKey other)) return false;
            return Objects.equals(companyId, other.companyId) && Objects.equals(productId, other.productId);
        }

        @Override
        public int hashCode() { return Objects.hash(companyId, productId); }
    }
}