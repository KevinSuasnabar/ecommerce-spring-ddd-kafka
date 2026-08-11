package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(CompanyId companyId, OrderId orderId);
}
