package com.ecommerce.order.infrastructure.adapter.out.inventory;

import com.ecommerce.order.application.port.out.InventoryPort;
import com.ecommerce.order.domain.model.ProductId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class FakeInventoryAdapter implements InventoryPort {

    private static final ProductId NOTEBOOK = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final ProductId MOUSE = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000002"));

    private final ConcurrentMap<ProductId, Integer> stock = new ConcurrentHashMap<>(Map.of(
            NOTEBOOK, 100,
            MOUSE, 50
    ));

    @Override
    public synchronized boolean reserve(ProductId productId, int quantity) {
        Integer available = stock.get(productId);
        if (available == null || available < quantity) {
            return false;
        }
        stock.put(productId, available - quantity);
        return true;
    }
}
