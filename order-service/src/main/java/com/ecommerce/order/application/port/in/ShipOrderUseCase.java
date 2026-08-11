package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;

public interface ShipOrderUseCase {

    void shipOrder(CompanyId companyId, OrderId orderId);
}
