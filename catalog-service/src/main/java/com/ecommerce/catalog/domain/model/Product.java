package com.ecommerce.catalog.domain.model;

import com.ecommerce.catalog.domain.event.DomainEvent;
import com.ecommerce.catalog.domain.event.ProductActivatedEvent;
import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.event.ProductPriceChangedEvent;
import com.ecommerce.catalog.domain.event.ProductRetiredEvent;
import com.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.ecommerce.catalog.domain.exception.InvalidProductTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Product {

    private final ProductId id;
    private String name;
    private String description;
    private Money price;
    private ProductStatus status;
    private final Set<CategoryId> categories = new HashSet<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;
    private final CompanyId companyId;

    private Product(ProductId id,
                    String name,
                    String description,
                    Money price,
                    ProductStatus status,
                    CompanyId companyId,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.companyId = companyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(ProductId id, String name, String description, Money price, CompanyId companyId) {
        Objects.requireNonNull(id, "id must not be null");
        requireNonBlank(name, "name");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(companyId, "CompanyId must not be null");

        Instant now = Instant.now();
        Product product = new Product(id, name, description, price, ProductStatus.DRAFT, companyId, now, now);
        product.recordEvent(new ProductCreatedEvent(companyId, id, name, price, ProductStatus.DRAFT, now));
        return product;
    }

    public static Product reconstitute(ProductId id, String name, String description,
                                       Money price, CompanyId companyId, ProductStatus status,
                                       Set<CategoryId> categoryIds, Instant createdAt, Instant updatedAt) {
        Objects.requireNonNull(id, "id must not be null");
        requireNonBlank(name, "name");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(companyId, "CompanyId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(categoryIds, "categoryIds must not be null");
        Product product = new Product(id, name, description, price, status, companyId, createdAt, updatedAt);
        product.categories.addAll(categoryIds);
        return product;
    }

    public void update(String name, String description) {
        requireNonBlank(name, "name");
        if (status == ProductStatus.RETIRED) {
            throw new InvalidProductTransitionException(id, "cannot update a retired product");
        }
        this.name = name;
        this.description = description;
        touch();
        recordEvent(new ProductUpdatedEvent(companyId, id, name, price, status, updatedAt));
    }

    public void changePrice(Money newPrice) {
        Objects.requireNonNull(newPrice, "newPrice must not be null");
        if (status == ProductStatus.RETIRED) {
            throw new InvalidProductTransitionException(id, "cannot change the price of a retired product");
        }
        Money oldPrice = price;
        this.price = newPrice;
        touch();
        recordEvent(new ProductPriceChangedEvent(companyId, id, name, oldPrice, newPrice, status, updatedAt));
    }

    public void activate() {
        transitionTo(ProductStatus.ACTIVE);
    }

    public void retire() {
        transitionTo(ProductStatus.RETIRED);
    }

    public void assignCategory(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        categories.add(categoryId);
    }

    public void removeCategory(CategoryId categoryId) {
        categories.remove(categoryId);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void transitionTo(ProductStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidProductTransitionException(id, "cannot transition from " + status + " to " + target);
        }
        this.status = target;
        touch();
        if (target == ProductStatus.ACTIVE) {
            recordEvent(new ProductActivatedEvent(companyId, id, name, price, status, updatedAt));
        } else {
            recordEvent(new ProductRetiredEvent(companyId, id, name, price, status, updatedAt));
        }
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    public ProductId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Money price() {
        return price;
    }

    public ProductStatus status() {
        return status;
    }

    public Set<CategoryId> categories() {
        return Set.copyOf(categories);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public CompanyId companyId() {
        return companyId;
    }
}
