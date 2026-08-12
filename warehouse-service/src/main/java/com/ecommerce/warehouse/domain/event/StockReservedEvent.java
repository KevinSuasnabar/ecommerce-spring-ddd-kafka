package com.ecommerce.warehouse.domain.event;

import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.StockId;

public record StockReservedEvent(StockId stockId, Quantity quantity) implements StockEvent {
}
