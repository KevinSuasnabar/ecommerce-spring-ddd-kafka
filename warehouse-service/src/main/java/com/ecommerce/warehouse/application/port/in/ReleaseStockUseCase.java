package com.ecommerce.warehouse.application.port.in;

import com.ecommerce.warehouse.application.dto.ReleaseStockCommand;

public interface ReleaseStockUseCase {

    void releaseStock(ReleaseStockCommand command);
}
