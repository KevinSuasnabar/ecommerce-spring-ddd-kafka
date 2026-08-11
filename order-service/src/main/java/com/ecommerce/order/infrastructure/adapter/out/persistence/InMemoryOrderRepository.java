package com.ecommerce.order.infrastructure.adapter.out.persistence;

import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private record Key(CompanyId companyId, OrderId orderId) {
    }

    private final ConcurrentMap<Key, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(new Key(order.companyId(), order.id()), order);
    }

    @Override
    public Optional<Order> findById(CompanyId companyId, OrderId orderId) {
        return Optional.ofNullable(store.get(new Key(companyId, orderId)));
    }
}
