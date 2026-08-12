package com.ecommerce.warehouse.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    void wrapsAUuid() {
        UUID uuid = UUID.randomUUID();

        ProductId productId = new ProductId(uuid);

        assertThat(productId.value()).isEqualTo(uuid);
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("product id must not be null");
    }

    @Test
    void equalValuesAreEqual() {
        UUID uuid = UUID.randomUUID();

        assertThat(new ProductId(uuid)).isEqualTo(new ProductId(uuid));
    }
}
