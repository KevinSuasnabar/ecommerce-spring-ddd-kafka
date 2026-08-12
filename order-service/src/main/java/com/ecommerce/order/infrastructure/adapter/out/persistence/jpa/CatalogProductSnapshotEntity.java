package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_product_snapshot", schema = "orders")
@IdClass(CatalogProductSnapshotKey.class)
public class CatalogProductSnapshotEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogProductSnapshotEntity() {
    }

    public CatalogProductSnapshotEntity(UUID productId, UUID companyId, String productName,
                                        BigDecimal price, String currency, String status, Instant updatedAt) {
        this.productId = productId;
        this.companyId = companyId;
        this.productName = productName;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID getProductId() { return productId; }
    public UUID getCompanyId() { return companyId; }
    public String getProductName() { return productName; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
