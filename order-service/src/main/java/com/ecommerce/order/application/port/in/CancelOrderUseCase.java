package com.ecommerce.order.application.port.in;

import com.ecommerce.order.application.dto.CancelOrderCommand;
import com.ecommerce.order.domain.model.CompanyId;

public interface CancelOrderUseCase {

    void cancelOrder(CompanyId companyId, CancelOrderCommand command);
}
