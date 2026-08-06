package com.ecommerce.order.application.port.out;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.domain.model.ProductId;

import java.util.Optional;

public interface CatalogProductStore {

    void upsert(CatalogProduct product);

    Optional<CatalogProduct> findById(ProductId productId);
}
