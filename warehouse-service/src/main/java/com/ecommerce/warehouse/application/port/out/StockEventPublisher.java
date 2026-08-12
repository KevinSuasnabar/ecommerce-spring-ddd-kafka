package com.ecommerce.warehouse.application.port.out;

import com.ecommerce.warehouse.domain.event.StockEvent;

public interface StockEventPublisher {

    void publish(StockEvent event);
}
