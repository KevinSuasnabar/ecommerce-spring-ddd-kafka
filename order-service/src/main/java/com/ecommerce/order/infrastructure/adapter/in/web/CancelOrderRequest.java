package com.ecommerce.order.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(@NotBlank String reason) {
}
