package com.ecommerce.order.application.port.in;

import com.ecommerce.order.application.dto.CancelOrderCommand;

public interface CancelOrderUseCase {

    void cancelOrder(CancelOrderCommand command);
}
