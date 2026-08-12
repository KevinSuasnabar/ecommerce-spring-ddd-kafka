package com.ecommerce.warehouse.domain.model;

import com.ecommerce.warehouse.domain.event.StockEvent;
import com.ecommerce.warehouse.domain.event.StockReceivedEvent;
import com.ecommerce.warehouse.domain.event.StockReleasedEvent;
import com.ecommerce.warehouse.domain.event.StockReservedEvent;
import com.ecommerce.warehouse.domain.exception.InsufficientStockException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Stock {

    private final StockId id;
    private final List<StockMovement> movements = new ArrayList<>();
    private final List<StockEvent> domainEvents = new ArrayList<>();

    private Quantity baselineAvailable = new Quantity(0);
    private Quantity baselineReserved = new Quantity(0);

    private Stock(StockId id) {
        this.id = id;
    }

    public static Stock create(StockId id) {
        Objects.requireNonNull(id, "stock id must not be null");
        return new Stock(id);
    }

    public static Stock reconstitute(StockId id, List<StockMovement> movements) {
        Objects.requireNonNull(id, "stock id must not be null");
        Objects.requireNonNull(movements, "movements must not be null");
        Stock stock = new Stock(id);
        stock.movements.addAll(movements);
        return stock;
    }

    public static Stock fromSnapshot(StockId id, int available, int reserved) {
        Objects.requireNonNull(id, "stock id must not be null");
        Stock stock = new Stock(id);
        stock.baselineAvailable = new Quantity(available);
        stock.baselineReserved = new Quantity(reserved);
        return stock;
    }

    public void receive(Quantity qty) {
        Objects.requireNonNull(qty, "quantity must not be null");
        movements.add(new StockMovement(StockMovementType.RECEIVED, qty, Instant.now()));
        domainEvents.add(new StockReceivedEvent(id, qty));
    }

    public void reserve(Quantity qty) {
        Objects.requireNonNull(qty, "quantity must not be null");
        Quantity available = available();
        if (available.isLessThan(qty)) {
            throw new InsufficientStockException(id, qty.value(), available.value());
        }
        movements.add(new StockMovement(StockMovementType.RESERVED, qty, Instant.now()));
        domainEvents.add(new StockReservedEvent(id, qty));
    }

    public void release(Quantity qty) {
        Objects.requireNonNull(qty, "quantity must not be null");
        Quantity reserved = reserved();
        if (reserved.isLessThan(qty)) {
            throw new InsufficientStockException(id, qty.value(), reserved.value(), true);
        }
        movements.add(new StockMovement(StockMovementType.RELEASED, qty, Instant.now()));
        domainEvents.add(new StockReleasedEvent(id, qty));
    }

    public List<StockEvent> pullDomainEvents() {
        List<StockEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public StockId id() {
        return id;
    }

    public List<StockMovement> movements() {
        return List.copyOf(movements);
    }

    public Quantity available() {
        return baselineAvailable
                .add(totalOfType(StockMovementType.RECEIVED))
                .add(totalOfType(StockMovementType.RELEASED))
                .subtract(totalOfType(StockMovementType.RESERVED));
    }

    public Quantity reserved() {
        return baselineReserved
                .add(totalOfType(StockMovementType.RESERVED))
                .subtract(totalOfType(StockMovementType.RELEASED));
    }

    private Quantity totalOfType(StockMovementType type) {
        return movements.stream()
                .filter(m -> m.type() == type)
                .map(StockMovement::quantity)
                .reduce(new Quantity(0), Quantity::add);
    }
}