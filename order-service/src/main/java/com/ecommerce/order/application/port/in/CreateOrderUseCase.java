package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.application.dto.CreateOrderCommand;

public interface CreateOrderUseCase {

    OrderId createOrder(CreateOrderCommand command);
}
