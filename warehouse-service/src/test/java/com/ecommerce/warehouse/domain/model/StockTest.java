package com.ecommerce.warehouse.domain.model;

import com.ecommerce.warehouse.domain.event.StockEvent;
import com.ecommerce.warehouse.domain.event.StockReceivedEvent;
import com.ecommerce.warehouse.domain.event.StockReleasedEvent;
import com.ecommerce.warehouse.domain.event.StockReservedEvent;
import com.ecommerce.warehouse.domain.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final StockId STOCK_ID = new StockId(COMPANY, PRODUCT);

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = Stock.create(STOCK_ID);
    }

    @Test
    void createStartsWithZeroAvailableAndReserved() {
        assertThat(stock.available()).isEqualTo(new Quantity(0));
        assertThat(stock.reserved()).isEqualTo(new Quantity(0));
        assertThat(stock.movements()).isEmpty();
    }

    @Test
    void createRejectsNullId() {
        assertThatThrownBy(() -> Stock.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("stock id must not be null");
    }

    @Test
    void fromSnapshotStartsWithGivenBalances() {
        Stock snapshot = Stock.fromSnapshot(STOCK_ID, 5, 2);

        assertThat(snapshot.available()).isEqualTo(new Quantity(5));
        assertThat(snapshot.reserved()).isEqualTo(new Quantity(2));
        assertThat(snapshot.movements()).isEmpty();
    }

    @Test
    void fromSnapshotRejectsNullId() {
        assertThatThrownBy(() -> Stock.fromSnapshot(null, 5, 2))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("stock id must not be null");
    }

    @Test
    void fromSnapshotKeepsBaselineWhenReceiving() {
        Stock snapshot = Stock.fromSnapshot(STOCK_ID, 10, 0);

        snapshot.receive(new Quantity(5));

        assertThat(snapshot.available()).isEqualTo(new Quantity(15));
        assertThat(snapshot.reserved()).isEqualTo(new Quantity(0));
        assertThat(snapshot.movements()).hasSize(1);
    }

    @Test
    void fromSnapshotKeepsBaselineWhenReservingAndReleasing() {
        Stock snapshot = Stock.fromSnapshot(STOCK_ID, 20, 5);

        snapshot.reserve(new Quantity(3));
        snapshot.release(new Quantity(2));

        assertThat(snapshot.reserved()).isEqualTo(new Quantity(6));
        assertThat(snapshot.available()).isEqualTo(new Quantity(19));
        assertThat(snapshot.movements()).hasSize(2);
    }

    @Test
    void receiveIncreasesAvailableAndEmitsEvent() {
        stock.receive(new Quantity(5));

        assertThat(stock.available()).isEqualTo(new Quantity(5));
        assertThat(stock.reserved()).isEqualTo(new Quantity(0));
        assertThat(stock.movements()).hasSize(1);
        assertThat(stock.movements().get(0).type()).isEqualTo(StockMovementType.RECEIVED);
        assertThat(stock.pullDomainEvents())
                .hasSize(1)
                .allMatch(event -> event instanceof StockReceivedEvent);
    }

    @Test
    void reserveMovesQuantityFromAvailableToReserved() {
        stock.receive(new Quantity(10));

        stock.reserve(new Quantity(4));

        assertThat(stock.available()).isEqualTo(new Quantity(6));
        assertThat(stock.reserved()).isEqualTo(new Quantity(4));
        assertThat(stock.pullDomainEvents())
                .anyMatch(event -> event instanceof StockReservedEvent r && r.quantity().equals(new Quantity(4)));
    }

    @Test
    void reserveMoreThanAvailableThrows() {
        stock.receive(new Quantity(3));

        assertThatThrownBy(() -> stock.reserve(new Quantity(5)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stock.available()).isEqualTo(new Quantity(3));
        assertThat(stock.reserved()).isEqualTo(new Quantity(0));
    }

    @Test
    void releaseReturnsReservedBackToAvailable() {
        stock.receive(new Quantity(10));
        stock.reserve(new Quantity(4));

        stock.release(new Quantity(1));

        assertThat(stock.available()).isEqualTo(new Quantity(7));
        assertThat(stock.reserved()).isEqualTo(new Quantity(3));
        assertThat(stock.pullDomainEvents())
                .anyMatch(event -> event instanceof StockReleasedEvent r && r.quantity().equals(new Quantity(1)));
    }

    @Test
    void releaseMoreThanReservedThrows() {
        stock.receive(new Quantity(10));
        stock.reserve(new Quantity(3));

        assertThatThrownBy(() -> stock.release(new Quantity(5)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stock.reserved()).isEqualTo(new Quantity(3));
    }

    @Test
    void fullCycle() {
        stock.receive(new Quantity(10));
        stock.reserve(new Quantity(3));
        stock.release(new Quantity(1));

        assertThat(stock.available()).isEqualTo(new Quantity(8));
        assertThat(stock.reserved()).isEqualTo(new Quantity(2));
        assertThat(stock.movements()).hasSize(3);
    }

    @Test
    void movementsReturnsImmutableCopy() {
        stock.receive(new Quantity(5));

        assertThatThrownBy(() -> stock.movements().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pullDomainEventsClearsTheQueue() {
        stock.receive(new Quantity(3));

        List<StockEvent> firstPull = stock.pullDomainEvents();
        List<StockEvent> secondPull = stock.pullDomainEvents();

        assertThat(firstPull).isNotEmpty();
        assertThat(secondPull).isEmpty();
    }

    @Test
    void zeroQuantityReceivingAllowed() {
        stock.receive(new Quantity(0));

        assertThat(stock.available()).isEqualTo(new Quantity(0));
        assertThat(stock.movements()).hasSize(1);
    }
}
