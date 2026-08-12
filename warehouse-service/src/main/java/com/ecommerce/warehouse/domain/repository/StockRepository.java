package com.ecommerce.warehouse.domain.repository;

import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;

import java.util.Optional;

public interface StockRepository {

    void save(Stock stock);

    Optional<Stock> findById(StockId stockId);
}
