package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movement", schema = "warehouse")
public class StockMovementJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StockMovementJpaEntity() {
    }

    public StockMovementJpaEntity(UUID id, UUID companyId, UUID productId,
                                  String type, int quantity, Instant occurredAt) {
        this.id = id;
        this.companyId = companyId;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getProductId() { return productId; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public Instant getOccurredAt() { return occurredAt; }
}
