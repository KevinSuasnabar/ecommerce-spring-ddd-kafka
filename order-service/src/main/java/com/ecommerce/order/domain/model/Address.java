package com.ecommerce.order.domain.model;

import java.util.Objects;

public record Address(String street, String city, String state, String country, String zipCode) {

    public Address {
        requireNonBlank(street, "street");
        requireNonBlank(city, "city");
        requireNonBlank(country, "country");
        requireNonBlank(zipCode, "zipCode");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Address)) {
            return false;
        }
        Address that = (Address) other;
        return street.equals(that.street)
                && city.equals(that.city)
                && Objects.equals(state, that.state)
                && country.equals(that.country)
                && zipCode.equals(that.zipCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, country, zipCode);
    }
}
