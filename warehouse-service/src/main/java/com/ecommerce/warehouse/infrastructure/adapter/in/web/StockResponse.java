package com.ecommerce.warehouse.infrastructure.adapter.in.web;

import com.ecommerce.warehouse.application.dto.StockQueryResult;

import java.util.List;
import java.util.UUID;

public record StockResponse(
        UUID productId,
        UUID companyId,
        int available,
        int reserved,
        List<StockMovementResponse> movements) {

    public record StockMovementResponse(String type, int quantity, String occurredAt) {
    }

    public static StockResponse from(StockQueryResult result) {
        List<StockMovementResponse> movements = result.movements().stream()
                .map(m -> new StockMovementResponse(m.type().name(), m.quantity(), m.occurredAt().toString()))
                .toList();
        return new StockResponse(
                result.stockId().productId().value(),
                result.stockId().companyId().value(),
                result.available(),
                result.reserved(),
                movements);
    }
}
