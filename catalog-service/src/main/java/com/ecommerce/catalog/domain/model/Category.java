package com.ecommerce.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Category {

    private final CategoryId id;
    private String name;
    private final CategoryId parentId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Category(CategoryId id, String name, CategoryId parentId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(CategoryId id, String name, CategoryId parentId) {
        Objects.requireNonNull(id, "id must not be null");
        requireNonBlank(name, "name");

        Instant now = Instant.now();
        return new Category(id, name, parentId, now, now);
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
