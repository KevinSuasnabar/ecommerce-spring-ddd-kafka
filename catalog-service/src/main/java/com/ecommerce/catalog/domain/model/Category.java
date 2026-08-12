package com.ecommerce.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Category {

    private final CategoryId id;
    private final CompanyId companyId;
    private String name;
    private final CategoryId parentId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Category(CategoryId id, CompanyId companyId, String name, CategoryId parentId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(CategoryId id, String name, CategoryId parentId, CompanyId companyId) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(companyId, "company id must not be null");
        requireNonBlank(name, "name");

        Instant now = Instant.now();
        return new Category(id, companyId, name, parentId, now, now);
    }

    public static Category reconstitute(CategoryId id, CompanyId companyId, String name,
                                        CategoryId parentId, Instant createdAt, Instant updatedAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(companyId, "company id must not be null");
        requireNonBlank(name, "name");
        return new Category(id, companyId, name, parentId, createdAt, updatedAt);
    }

    public void rename(String name) {
        requireNonBlank(name, "name");
        this.name = name;
        this.updatedAt = Instant.now();
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    public CategoryId id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String name() {
        return name;
    }

    public CategoryId parentId() {
        return parentId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
