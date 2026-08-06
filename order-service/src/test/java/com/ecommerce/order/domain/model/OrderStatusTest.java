package com.ecommerce.order.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void createdCanGoToConfirmedOrCancelled() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void confirmedCanGoToShippedOrCancelled() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }

    @Test
    void shippedCanOnlyGoToDelivered() {
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }

    @Test
    void deliveredAndCancelledAreTerminalStates() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }
}
