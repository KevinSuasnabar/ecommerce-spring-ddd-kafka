package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "catalog")
public class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(
            name = "product_categories",
            schema = "catalog",
            joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "category_id")
    private Set<UUID> categoryIds = new HashSet<>();

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(UUID id, UUID companyId, String name, String description,
                            BigDecimal price, String currency, String status,
                            Instant createdAt, Instant updatedAt, Set<UUID> categoryIds) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.categoryIds = categoryIds != null ? categoryIds : new HashSet<>();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<UUID> getCategoryIds() { return categoryIds; }
}
