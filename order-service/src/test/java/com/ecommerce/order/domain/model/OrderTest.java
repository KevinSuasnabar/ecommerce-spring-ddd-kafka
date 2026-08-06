package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCancelledEvent;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.event.OrderDeliveredEvent;
import com.ecommerce.order.domain.event.OrderShippedEvent;
import com.ecommerce.order.domain.exception.InvalidOrderTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final CustomerId CUSTOMER = new CustomerId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final Address ADDRESS = new Address("Av. Siempre Viva 123", "Springfield", null, "AR", "1406");
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final OrderLine LINE = new OrderLine(
            PRODUCT, "Notebook", 2, new Money(new BigDecimal("1500.00"), USD));
    private static final OrderLine SECOND_LINE = new OrderLine(
            PRODUCT, "Mouse", 1, new Money(new BigDecimal("50.00"), USD));

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.create(OrderId.newId(), CUSTOMER, List.of(LINE, SECOND_LINE), ADDRESS, PaymentMethod.CREDIT_CARD);
    }

    @Test
    void createBuildsOrderInCreatedStateWithComputedTotal() {
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.customerId()).isEqualTo(CUSTOMER);
        assertThat(order.total().amount()).isEqualByComparingTo("3050.00");
        assertThat(order.total().currency()).isEqualTo(USD);
    }

    @Test
    void createEmitsCreatedEvent() {
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .allMatch(event -> event instanceof OrderCreatedEvent);
    }

    @Test
    void createWithNoLinesIsRejected() {
        assertThatThrownBy(() -> Order.create(OrderId.newId(), CUSTOMER, List.of(), ADDRESS, PaymentMethod.CREDIT_CARD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    void orderLineRejectsZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> new OrderLine(PRODUCT, "Mouse", 0, new Money(new BigDecimal("10.00"), USD)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderLine(PRODUCT, "Mouse", -1, new Money(new BigDecimal("10.00"), USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmTransitionsToConfirmedAndEmitsEvent() {
        order.confirm();

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.pullDomainEvents())
                .anyMatch(event -> event instanceof OrderConfirmedEvent);
    }

    @Test
    void shipFromCreatedIsRejected() {
        assertThatThrownBy(order::ship)
                .isInstanceOf(InvalidOrderTransitionException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void fullLifecycleFromCreatedToDelivered() {
        order.confirm();
        order.ship();
        order.deliver();

        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.pullDomainEvents())
                .extracting(event -> event.getClass().getSimpleName())
                .containsExactlyInAnyOrder(
                        "OrderCreatedEvent",
                        "OrderConfirmedEvent",
                        "OrderShippedEvent",
                        "OrderDeliveredEvent");
    }

    @Test
    void cancelFromCreatedWorksWithReason() {
        order.cancel("changed my mind");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.pullDomainEvents())
                .anyMatch(event -> event instanceof OrderCancelledEvent cancelled && "changed my mind".equals(cancelled.reason()));
    }

    @Test
    void cancelFromShippedIsRejected() {
        order.confirm();
        order.ship();

        assertThatThrownBy(() -> order.cancel("too late"))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }

    @Test
    void cancelWithoutReasonIsRejected() {
        assertThatThrownBy(() -> order.cancel("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pullingDomainEventsClearsTheQueue() {
        order.confirm();

        List<DomainEvent> firstPull = order.pullDomainEvents();
        List<DomainEvent> secondPull = order.pullDomainEvents();

        assertThat(firstPull).isNotEmpty();
        assertThat(secondPull).isEmpty();
    }

    @Test
    void linesAreImmutableAfterCreation() {
        assertThatThrownBy(() -> order.lines().add(SECOND_LINE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
