package com.ecommerce.warehouse.application.dto;

import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.model.StockMovement;
import com.ecommerce.warehouse.domain.model.StockMovementType;

import java.time.Instant;
import java.util.List;

public record StockQueryResult(
        StockId stockId,
        int available,
        int reserved,
        List<StockMovementResult> movements) {

    public record StockMovementResult(StockMovementType type, int quantity, Instant occurredAt) {
    }

    public static StockQueryResult from(Stock stock) {
        List<StockMovementResult> movementResults = stock.movements().stream()
                .map(StockQueryResult::toMovementResult)
                .toList();
        return new StockQueryResult(
                stock.id(),
                stock.available().value(),
                stock.reserved().value(),
                movementResults);
    }

    public static StockQueryResult of(StockId stockId, int available, int reserved) {
        return new StockQueryResult(stockId, available, reserved, List.of());
    }

    public static StockMovementResult toMovementResult(StockMovement movement) {
        return new StockMovementResult(movement.type(), movement.quantity().value(), movement.occurredAt());
    }
}