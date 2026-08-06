package com.ecommerce.order.infrastructure.adapter.in.web;

import com.ecommerce.order.domain.model.OrderId;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId) {

    public static CreateOrderResponse from(OrderId orderId) {
        return new CreateOrderResponse(orderId.value());
    }
}
