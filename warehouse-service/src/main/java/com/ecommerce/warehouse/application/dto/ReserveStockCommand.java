package com.ecommerce.warehouse.application.dto;

import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.StockId;

public record ReserveStockCommand(StockId stockId, Quantity quantity) {
}
