package com.ecommerce.catalog.infrastructure.adapter.out.persistence;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("!postgres")
@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private record Key(CompanyId companyId, CategoryId categoryId) {
    }

    private final ConcurrentMap<Key, Category> store = new ConcurrentHashMap<>();

    @Override
    public void save(Category category) {
        store.put(new Key(category.companyId(), category.id()), category);
    }

    @Override
    public Optional<Category> findById(CompanyId companyId, CategoryId categoryId) {
        return Optional.ofNullable(store.get(new Key(companyId, categoryId)));
    }

    @Override
    public List<Category> findAllByCompanyId(CompanyId companyId) {
        return store.entrySet().stream()
                .filter(entry -> entry.getKey().companyId().equals(companyId))
                .map(entry -> entry.getValue())
                .toList();
    }

    @Override
    public boolean existsByName(CompanyId companyId, String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return store.entrySet().stream()
                .filter(entry -> entry.getKey().companyId().equals(companyId))
                .map(entry -> entry.getValue())
                .anyMatch(category -> category.name().toLowerCase(Locale.ROOT).equals(normalized));
    }
}
