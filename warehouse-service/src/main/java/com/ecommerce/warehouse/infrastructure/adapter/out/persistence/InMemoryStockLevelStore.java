package com.ecommerce.warehouse.infrastructure.adapter.out.persistence;

import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.model.StockId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Profile("!postgres")
public class InMemoryStockLevelStore implements StockLevelStore {

    private record Key(UUID companyId, UUID productId) {}

    private final ConcurrentMap<Key, StockLevel> levels = new ConcurrentHashMap<>();

    @Override
    public void upsert(StockId stockId, int available, int reserved) {
        levels.put(new Key(stockId.companyId().value(), stockId.productId().value()),
                new StockLevel(available, reserved));
    }

    @Override
    public Optional<StockLevel> findByStockId(StockId stockId) {
        return Optional.ofNullable(levels.get(new Key(stockId.companyId().value(), stockId.productId().value())));
    }
}