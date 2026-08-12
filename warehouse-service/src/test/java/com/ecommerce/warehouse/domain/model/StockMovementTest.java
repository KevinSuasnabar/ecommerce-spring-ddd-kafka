package com.ecommerce.warehouse.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementTest {

    @Test
    void createsWithTypeQuantityAndTimestamp() {
        Quantity qty = new Quantity(5);
        Instant now = Instant.now();

        StockMovement movement = new StockMovement(StockMovementType.RECEIVED, qty, now);

        assertThat(movement.type()).isEqualTo(StockMovementType.RECEIVED);
        assertThat(movement.quantity()).isEqualTo(qty);
        assertThat(movement.occurredAt()).isEqualTo(now);
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> new StockMovement(null, new Quantity(1), Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("movement type must not be null");
    }

    @Test
    void rejectsNullQuantity() {
        assertThatThrownBy(() -> new StockMovement(StockMovementType.RECEIVED, null, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("quantity must not be null");
    }
}
