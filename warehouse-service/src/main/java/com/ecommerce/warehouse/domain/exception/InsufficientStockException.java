package com.ecommerce.warehouse.domain.exception;

import com.ecommerce.warehouse.domain.model.StockId;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(StockId stockId, int requested, int available) {
        super("Insufficient stock for " + stockId + ": requested " + requested + ", available " + available);
    }

    public InsufficientStockException(StockId stockId, int requested, int reserved, boolean forRelease) {
        super("Cannot release " + requested + " for " + stockId + ": only " + reserved + " reserved");
    }
}
