package com.ecommerce.catalog.infrastructure.adapter.out.persistence;

import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("!postgres")
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private record Key(CompanyId companyId, ProductId productId) {
    }

    private final ConcurrentMap<Key, Product> store = new ConcurrentHashMap<>();

    @Override
    public void save(Product product) {
        store.put(new Key(product.companyId(), product.id()), product);
    }

    @Override
    public Optional<Product> findById(CompanyId companyId, ProductId productId) {
        return Optional.ofNullable(store.get(new Key(companyId, productId)));
    }

    @Override
    public List<Product> findAllByCompanyId(CompanyId companyId) {
        return store.entrySet().stream()
                .filter(entry -> entry.getKey().companyId().equals(companyId))
                .map(Map.Entry::getValue)
                .toList();
    }
}
