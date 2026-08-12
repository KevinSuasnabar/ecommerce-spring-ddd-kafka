package com.ecommerce.warehouse.application.port.in;

import com.ecommerce.warehouse.application.dto.ReserveStockCommand;

public interface ReserveStockUseCase {

    void reserveStock(ReserveStockCommand command);
}
