package com.ecommerce.order.infrastructure.adapter.out.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentMap<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.id(), order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}
