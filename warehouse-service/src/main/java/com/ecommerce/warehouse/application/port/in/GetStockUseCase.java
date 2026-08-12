package com.ecommerce.warehouse.application.port.in;

import com.ecommerce.warehouse.application.dto.StockQueryResult;
import com.ecommerce.warehouse.domain.model.StockId;

public interface GetStockUseCase {

    StockQueryResult getStock(StockId stockId);
}
