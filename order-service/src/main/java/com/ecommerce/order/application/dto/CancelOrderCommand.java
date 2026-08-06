package com.ecommerce.order.application.dto;

import com.ecommerce.order.domain.model.OrderId;

public record CancelOrderCommand(OrderId orderId, String reason) {
}
