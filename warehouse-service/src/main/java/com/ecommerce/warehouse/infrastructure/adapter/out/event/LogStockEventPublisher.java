package com.ecommerce.warehouse.infrastructure.adapter.out.event;

import com.ecommerce.warehouse.application.port.out.StockEventPublisher;
import com.ecommerce.warehouse.domain.event.StockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogStockEventPublisher implements StockEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogStockEventPublisher.class);

    @Override
    public void publish(StockEvent event) {
        log.info("Published stock event: {}", event);
    }
}
