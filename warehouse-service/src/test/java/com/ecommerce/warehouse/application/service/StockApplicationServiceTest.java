package com.ecommerce.warehouse.application.service;

import com.ecommerce.warehouse.application.dto.ReceiveStockCommand;
import com.ecommerce.warehouse.application.dto.ReleaseStockCommand;
import com.ecommerce.warehouse.application.dto.ReserveStockCommand;
import com.ecommerce.warehouse.application.dto.StockQueryResult;
import com.ecommerce.warehouse.application.port.out.StockEventPublisher;
import com.ecommerce.warehouse.application.port.out.StockLevelStore;
import com.ecommerce.warehouse.domain.event.StockReceivedEvent;
import com.ecommerce.warehouse.domain.event.StockReleasedEvent;
import com.ecommerce.warehouse.domain.event.StockReservedEvent;
import com.ecommerce.warehouse.domain.exception.InsufficientStockException;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockApplicationServiceTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final StockId STOCK_ID = new StockId(COMPANY, PRODUCT);

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockEventPublisher eventPublisher;

    @Mock
    private StockLevelStore stockLevelStore;

    private StockApplicationService service;

    @BeforeEach
    void setUp() {
        service = new StockApplicationService(stockRepository, stockLevelStore, eventPublisher);
    }

    @Test
    void ensureStockExistsCreatesStockIfAbsent() {
        when(stockLevelStore.findByStockId(STOCK_ID)).thenReturn(Optional.empty());

        service.ensureStockExists(COMPANY, PRODUCT);

        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void ensureStockExistsDoesNothingIfPresent() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(0, 0)));

        service.ensureStockExists(COMPANY, PRODUCT);

        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    void receiveStockAddsQuantityAndPublishesEvent() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(0, 0)));

        service.receiveStock(new ReceiveStockCommand(STOCK_ID, new Quantity(10)));

        verify(stockRepository).save(argThat(saved -> saved.available().value() == 10));
        verify(eventPublisher).publish(any(StockReceivedEvent.class));
    }

    @Test
    void reserveStockLocksQuantityAndPublishesEvent() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(5, 0)));

        service.reserveStock(new ReserveStockCommand(STOCK_ID, new Quantity(3)));

        verify(stockRepository).save(argThat(saved -> saved.available().value() == 2));
        verify(stockRepository).save(argThat(saved -> saved.reserved().value() == 3));
        verify(eventPublisher).publish(any(StockReservedEvent.class));
    }

    @Test
    void reserveMoreThanAvailablePropagatesException() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(2, 0)));

        assertThatThrownBy(() -> service.reserveStock(new ReserveStockCommand(STOCK_ID, new Quantity(5))))
                .isInstanceOf(InsufficientStockException.class);

        verify(stockRepository, never()).save(any(Stock.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void releaseStockFreesReservedAndPublishesEvent() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(8, 2)));

        service.releaseStock(new ReleaseStockCommand(STOCK_ID, new Quantity(2)));

        verify(stockRepository).save(argThat(saved -> saved.reserved().value() == 0));
        verify(stockRepository).save(argThat(saved -> saved.available().value() == 10));
        verify(eventPublisher).publish(any(StockReleasedEvent.class));
    }

    @Test
    void getStockReturnsQueryResult() {
        when(stockLevelStore.findByStockId(STOCK_ID))
                .thenReturn(Optional.of(new StockLevelStore.StockLevel(5, 0)));

        StockQueryResult result = service.getStock(STOCK_ID);

        assertThat(result.stockId()).isEqualTo(STOCK_ID);
        assertThat(result.available()).isEqualTo(5);
        assertThat(result.reserved()).isZero();
        assertThat(result.movements()).isEmpty();
    }

    @Test
    void getStockMovementsReturnsFullLedger() {
        Stock stock = Stock.create(STOCK_ID);
        stock.receive(new Quantity(5));
        stock.pullDomainEvents();
        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));

        StockQueryResult result = service.getStockMovements(STOCK_ID);

        assertThat(result.stockId()).isEqualTo(STOCK_ID);
        assertThat(result.available()).isEqualTo(5);
        assertThat(result.movements()).hasSize(1);
    }
}
