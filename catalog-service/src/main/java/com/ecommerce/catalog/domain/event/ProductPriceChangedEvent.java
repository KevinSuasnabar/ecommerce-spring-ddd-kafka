package com.ecommerce.catalog.domain.event;

import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;

import java.time.Instant;

public record ProductPriceChangedEvent(CompanyId companyId, ProductId productId, String productName, Money oldPrice, Money newPrice,
                                       ProductStatus status, Instant occurredAt)
        implements DomainEvent {
}
