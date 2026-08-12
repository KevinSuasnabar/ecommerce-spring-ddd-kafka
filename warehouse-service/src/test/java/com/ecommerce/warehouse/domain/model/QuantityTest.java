package com.ecommerce.warehouse.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void zeroIsAllowed() {
        assertThat(new Quantity(0).value()).isZero();
    }

    @Test
    void rejectsNegativeValue() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void addsTwoQuantities() {
        assertThat(new Quantity(5).add(new Quantity(3))).isEqualTo(new Quantity(8));
    }

    @Test
    void subtractsSmallerQuantity() {
        assertThat(new Quantity(8).subtract(new Quantity(3))).isEqualTo(new Quantity(5));
    }

    @Test
    void rejectsSubtractingLargerQuantity() {
        assertThatThrownBy(() -> new Quantity(3).subtract(new Quantity(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("larger");
    }

    @Test
    void tellsWhenSmallerThanAnother() {
        assertThat(new Quantity(2).isLessThan(new Quantity(3))).isTrue();
        assertThat(new Quantity(3).isLessThan(new Quantity(3))).isFalse();
        assertThat(new Quantity(4).isLessThan(new Quantity(3))).isFalse();
    }
}