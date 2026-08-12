package com.ecommerce.warehouse.domain.model;

public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    public Quantity subtract(Quantity other) {
        if (isLessThan(other)) {
            throw new IllegalArgumentException("cannot subtract a larger quantity");
        }
        return new Quantity(value - other.value);
    }

    public boolean isLessThan(Quantity other) {
        return value < other.value;
    }
}