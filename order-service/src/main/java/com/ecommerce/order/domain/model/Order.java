package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.event.OrderCancelledEvent;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.event.OrderDeliveredEvent;
import com.ecommerce.order.domain.event.OrderShippedEvent;
import com.ecommerce.order.domain.exception.InvalidOrderTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Money total;
    private final Address shippingAddress;
    private final PaymentMethod paymentMethod;
    private OrderStatus status;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(OrderId id,
                  CustomerId customerId,
                  List<OrderLine> lines,
                  Address shippingAddress,
                  PaymentMethod paymentMethod,
                  OrderStatus status,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.total = computeTotal(lines);
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(OrderId id,
                               CustomerId customerId,
                               List<OrderLine> lines,
                               Address shippingAddress,
                               PaymentMethod paymentMethod) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must have at least one line");
        }
        Objects.requireNonNull(shippingAddress, "shippingAddress must not be null");
        Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");

        Instant now = Instant.now();
        Order order = new Order(id, customerId, lines, shippingAddress, paymentMethod,
                OrderStatus.CREATED, now, now);
        order.recordEvent(new OrderCreatedEvent(id, now));
        return order;
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
        recordEvent(new OrderConfirmedEvent(id, updatedAt));
    }

    public void ship() {
        transitionTo(OrderStatus.SHIPPED);
        recordEvent(new OrderShippedEvent(id, updatedAt));
    }

    public void deliver() {
        transitionTo(OrderStatus.DELIVERED);
        recordEvent(new OrderDeliveredEvent(id, updatedAt));
    }

    public void cancel(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("cancellation reason must not be blank");
        }
        transitionTo(OrderStatus.CANCELLED);
        recordEvent(new OrderCancelledEvent(id, reason, updatedAt));
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderTransitionException(id, status, target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private static Money computeTotal(List<OrderLine> lines) {
        Currency currency = lines.get(0).subtotal().currency();
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.zero(currency), Money::add);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public Money total() {
        return total;
    }

    public Address shippingAddress() {
        return shippingAddress;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
