package com.ecommerce.order.application.port.in;

import com.ecommerce.order.application.dto.OrderQueryResult;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;

public interface GetOrderUseCase {

    OrderQueryResult getOrder(CompanyId companyId, OrderId orderId);
}
