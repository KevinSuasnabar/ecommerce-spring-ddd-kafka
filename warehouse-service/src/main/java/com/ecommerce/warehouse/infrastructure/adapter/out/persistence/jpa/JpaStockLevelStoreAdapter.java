package com.ecommerce.warehouse.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.model.StockId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@Profile("postgres")
public class JpaStockLevelStoreAdapter implements StockLevelStore {

    private final JpaStockLevelRepository jpa;

    public JpaStockLevelStoreAdapter(JpaStockLevelRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void upsert(StockId stockId, int available, int reserved) {
        StockLevelEntity entity = jpa.findByCompanyIdAndProductId(
                stockId.companyId().value(), stockId.productId().value())
                .orElseGet(() -> new StockLevelEntity(
                        stockId.companyId().value(), stockId.productId().value(), 0, 0, Instant.now()));

        entity.setAvailable(available);
        entity.setReserved(reserved);
        entity.setUpdatedAt(Instant.now());
        jpa.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockLevel> findByStockId(StockId stockId) {
        return jpa.findByCompanyIdAndProductId(stockId.companyId().value(), stockId.productId().value())
                .map(e -> new StockLevel(e.getAvailable(), e.getReserved()));
    }
}