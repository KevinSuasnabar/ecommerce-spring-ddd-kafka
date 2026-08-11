package com.ecommerce.order.application.dto;

import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;

import java.util.List;

public record CreateOrderCommand(
        CompanyId companyId,
        CustomerId customerId,
        Address shippingAddress,
        List<OrderLineCommand> lines,
        PaymentMethod paymentMethod) {

    public record OrderLineCommand(ProductId productId, int quantity) {
    }
}
