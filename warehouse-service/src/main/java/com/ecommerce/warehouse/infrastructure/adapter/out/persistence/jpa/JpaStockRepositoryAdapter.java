package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.model.StockMovement;
import com.ecommerce.warehouse.domain.model.StockMovementType;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("postgres")
public class JpaStockRepositoryAdapter implements StockRepository {

    private final JpaStockRepository stockJpa;
    private final JpaStockMovementRepository movementJpa;
    private final StockLevelStore stockLevelStore;

    public JpaStockRepositoryAdapter(JpaStockRepository stockJpa,
                                     JpaStockMovementRepository movementJpa,
                                     StockLevelStore stockLevelStore) {
        this.stockJpa = stockJpa;
        this.movementJpa = movementJpa;
        this.stockLevelStore = stockLevelStore;
    }

    @Override
    @Transactional
    public void save(Stock stock) {
        UUID company = stock.id().companyId().value();
        UUID product = stock.id().productId().value();
        StockJpaEntity.StockKey key = new StockJpaEntity.StockKey(company, product);

        if (stockJpa.findById(key).isEmpty()) {
            stockJpa.save(new StockJpaEntity(company, product, Instant.now()));
        }

        List<StockMovementJpaEntity> movementEntities = stock.movements().stream()
                .map(m -> new StockMovementJpaEntity(
                        UUID.randomUUID(), company, product,
                        m.type().name(), m.quantity().value(), m.occurredAt()))
                .toList();
        movementJpa.saveAll(movementEntities);

        stockLevelStore.upsert(stock.id(), stock.available().value(), stock.reserved().value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stock> findById(StockId stockId) {
        UUID company = stockId.companyId().value();
        UUID product = stockId.productId().value();
        StockJpaEntity.StockKey key = new StockJpaEntity.StockKey(company, product);

        if (stockJpa.findById(key).isEmpty()) {
            return Optional.empty();
        }

        List<StockMovement> movements = movementJpa.findAllByCompanyIdAndProductId(company, product).stream()
                .map(m -> new StockMovement(
                        StockMovementType.valueOf(m.getType()),
                        new Quantity(m.getQuantity()),
                        m.getOccurredAt()))
                .toList();

        return Optional.of(Stock.reconstitute(stockId, movements));
    }
}