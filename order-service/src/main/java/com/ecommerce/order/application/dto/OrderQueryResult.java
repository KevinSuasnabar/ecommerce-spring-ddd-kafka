package com.ecommerce.order.application.dto;

import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderLine;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;

import java.time.Instant;
import java.util.List;

public record OrderQueryResult(
        OrderId id,
        CustomerId customerId,
        List<OrderLineResult> lines,
        Money total,
        Address shippingAddress,
        PaymentMethod paymentMethod,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public record OrderLineResult(ProductId productId, String productName, int quantity, Money unitPrice, Money subtotal) {
    }

    public static OrderQueryResult from(Order order) {
        List<OrderLineResult> lineResults = order.lines().stream()
                .map(OrderQueryResult::toLineResult)
                .toList();
        return new OrderQueryResult(
                order.id(),
                order.customerId(),
                lineResults,
                order.total(),
                order.shippingAddress(),
                order.paymentMethod(),
                order.status(),
                order.createdAt(),
                order.updatedAt());
    }

    private static OrderLineResult toLineResult(OrderLine line) {
        return new OrderLineResult(
                line.productId(),
                line.productName(),
                line.quantity(),
                line.unitPrice(),
                line.subtotal());
    }
}
