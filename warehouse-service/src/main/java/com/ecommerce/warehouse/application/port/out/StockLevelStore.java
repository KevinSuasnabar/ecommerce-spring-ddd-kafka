package com.ecommerce.warehouse.application.port.out;

import com.ecommerce.warehouse.domain.model.StockId;

import java.util.Optional;

public interface StockLevelStore {

    void upsert(StockId stockId, int available, int reserved);

    Optional<StockLevel> findByStockId(StockId stockId);

    record StockLevel(int available, int reserved) {
    }
}