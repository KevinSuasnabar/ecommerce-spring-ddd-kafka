package com.ecommerce.order.infrastructure.adapter.out.catalog;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.ProductId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryCatalogProductStore implements CatalogProductStore {

    private final ConcurrentMap<UUID, CatalogProduct> products = new ConcurrentHashMap<>();

    @Override
    public void upsert(CatalogProduct product) {
        products.put(product.productId().value(), product);
    }

    @Override
    public Optional<CatalogProduct> findById(ProductId productId) {
        return Optional.ofNullable(products.get(productId.value()));
    }

    public List<CatalogProduct> findAll() {
        return List.copyOf(products.values());
    }
}
