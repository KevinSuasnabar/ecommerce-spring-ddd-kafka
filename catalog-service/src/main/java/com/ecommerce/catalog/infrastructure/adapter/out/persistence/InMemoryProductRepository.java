package com.ecommerce.catalog.infrastructure.adapter.out.persistence;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentMap<ProductId, Product> store = new ConcurrentHashMap<>();

    @Override
    public void save(Product product) {
        store.put(product.id(), product);
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return Optional.ofNullable(store.get(productId));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(store.values());
    }
}
