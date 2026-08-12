package com.ecommerce.warehouse.application.port.in;

import com.ecommerce.warehouse.application.dto.ReceiveStockCommand;

public interface ReceiveStockUseCase {

    void receiveStock(ReceiveStockCommand command);
}
