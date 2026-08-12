package com.ecommerce.order.infrastructure.adapter.out.catalog;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.ProductId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("!postgres")
@Component
public class InMemoryCatalogProductStore implements CatalogProductStore {

    private record Key(CompanyId companyId, ProductId productId) {
    }

    private final ConcurrentMap<Key, CatalogProduct> products = new ConcurrentHashMap<>();

    @Override
    public void upsert(CatalogProduct product) {
        products.put(new Key(product.companyId(), product.productId()), product);
    }

    @Override
    public Optional<CatalogProduct> findById(CompanyId companyId, ProductId productId) {
        return Optional.ofNullable(products.get(new Key(companyId, productId)));
    }

    public List<CatalogProduct> findAll() {
        return List.copyOf(products.values());
    }
}
