package com.ecommerce.warehouse.application.service;

import com.ecommerce.warehouse.application.dto.ReceiveStockCommand;
import com.ecommerce.warehouse.application.dto.ReleaseStockCommand;
import com.ecommerce.warehouse.application.dto.ReserveStockCommand;
import com.ecommerce.warehouse.application.dto.StockQueryResult;
import com.ecommerce.warehouse.application.port.in.EnsureStockExistsUseCase;
import com.ecommerce.warehouse.application.port.in.GetStockMovementsUseCase;
import com.ecommerce.warehouse.application.port.in.GetStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReceiveStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReleaseStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReserveStockUseCase;
import com.ecommerce.warehouse.application.port.out.StockEventPublisher;
import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.event.StockEvent;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import org.springframework.stereotype.Service;

@Service
public class StockApplicationService
        implements EnsureStockExistsUseCase, ReceiveStockUseCase, ReserveStockUseCase,
        ReleaseStockUseCase, GetStockUseCase, GetStockMovementsUseCase {

    private final StockRepository stockRepository;
    private final StockLevelStore stockLevelStore;
    private final StockEventPublisher eventPublisher;

    public StockApplicationService(StockRepository stockRepository,
                                    StockLevelStore stockLevelStore,
                                    StockEventPublisher eventPublisher) {
        this.stockRepository = stockRepository;
        this.stockLevelStore = stockLevelStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void ensureStockExists(CompanyId companyId, ProductId productId) {
        StockId stockId = new StockId(companyId, productId);
        if (stockLevelStore.findByStockId(stockId).isEmpty()) {
            stockRepository.save(Stock.create(stockId));
        }
    }

    @Override
    public void receiveStock(ReceiveStockCommand command) {
        Stock stock = loadStock(command.stockId());
        stock.receive(command.quantity());
        stockRepository.save(stock);
        publishEvents(stock);
    }

    @Override
    public void reserveStock(ReserveStockCommand command) {
        Stock stock = loadStock(command.stockId());
        stock.reserve(command.quantity());
        stockRepository.save(stock);
        publishEvents(stock);
    }

    @Override
    public void releaseStock(ReleaseStockCommand command) {
        Stock stock = loadStock(command.stockId());
        stock.release(command.quantity());
        stockRepository.save(stock);
        publishEvents(stock);
    }

    @Override
    public StockQueryResult getStock(StockId stockId) {
        return stockLevelStore.findByStockId(stockId)
                .map(level -> StockQueryResult.of(stockId, level.available(), level.reserved()))
                .orElseGet(() -> StockQueryResult.of(stockId, 0, 0));
    }

    @Override
    public StockQueryResult getStockMovements(StockId stockId) {
        return StockQueryResult.from(loadFullLedger(stockId));
    }

    private Stock loadStock(StockId stockId) {
        return stockLevelStore.findByStockId(stockId)
                .map(level -> Stock.fromSnapshot(stockId, level.available(), level.reserved()))
                .orElseGet(() -> Stock.create(stockId));
    }

    private Stock loadFullLedger(StockId stockId) {
        return stockRepository.findById(stockId)
                .orElseGet(() -> Stock.create(stockId));
    }

    private void publishEvents(Stock stock) {
        for (StockEvent event : stock.pullDomainEvents()) {
            eventPublisher.publish(event);
        }
    }
}