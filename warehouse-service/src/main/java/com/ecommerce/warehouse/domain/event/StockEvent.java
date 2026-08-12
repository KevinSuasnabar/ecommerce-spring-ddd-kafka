package com.ecommerce.warehouse.domain.event;

import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.StockId;

public sealed interface StockEvent
        permits StockReceivedEvent, StockReservedEvent, StockReleasedEvent {

    StockId stockId();
    Quantity quantity();
}
