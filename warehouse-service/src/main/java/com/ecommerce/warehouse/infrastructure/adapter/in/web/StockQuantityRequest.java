package com.ecommerce.warehouse.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Min;

public record StockQuantityRequest(@Min(1) int quantity) {
}
