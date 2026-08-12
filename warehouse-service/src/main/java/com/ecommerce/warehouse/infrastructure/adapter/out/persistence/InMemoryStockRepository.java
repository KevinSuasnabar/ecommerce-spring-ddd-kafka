package com.ecommerce.warehouse.infrastructure.adapter.out.persistence;

import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.model.StockMovement;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("!postgres")
@Repository
public class InMemoryStockRepository implements StockRepository {

    private record Key(StockId stockId) {
    }

    private final ConcurrentMap<Key, List<StockMovement>> ledger = new ConcurrentHashMap<>();
    private final StockLevelStore stockLevelStore;

    public InMemoryStockRepository(StockLevelStore stockLevelStore) {
        this.stockLevelStore = stockLevelStore;
    }

    @Override
    public void save(Stock stock) {
        ledger.merge(new Key(stock.id()), new ArrayList<>(stock.movements()),
                (existing, added) -> {
                    List<StockMovement> merged = new ArrayList<>(existing);
                    merged.addAll(added);
                    return merged;
                });
        stockLevelStore.upsert(stock.id(), stock.available().value(), stock.reserved().value());
    }

    @Override
    public Optional<Stock> findById(StockId stockId) {
        List<StockMovement> movements = ledger.get(new Key(stockId));
        if (movements == null) {
            return Optional.empty();
        }
        return Optional.of(Stock.reconstitute(stockId, List.copyOf(movements)));
    }
}
