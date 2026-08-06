package com.ecommerce.order.infrastructure.adapter.in.web;

import com.ecommerce.order.application.dto.OrderQueryResult;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        List<LineResponse> lines,
        BigDecimal total,
        Currency currency,
        AddressResponse shippingAddress,
        PaymentMethod paymentMethod,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(OrderQueryResult result) {
        return new OrderResponse(
                result.id().value(),
                result.customerId().value(),
                result.lines().stream()
                        .map(line -> new LineResponse(
                                line.productId().value(),
                                line.productName(),
                                line.quantity(),
                                line.unitPrice().amount(),
                                line.subtotal().amount()))
                        .toList(),
                result.total().amount(),
                result.total().currency(),
                new AddressResponse(
                        result.shippingAddress().street(),
                        result.shippingAddress().city(),
                        result.shippingAddress().state(),
                        result.shippingAddress().country(),
                        result.shippingAddress().zipCode()),
                result.paymentMethod(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }

    public record LineResponse(UUID productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public record AddressResponse(String street, String city, String state, String country, String zipCode) {
    }
}
