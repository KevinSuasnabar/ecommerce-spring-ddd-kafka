package com.ecommerce.catalog.infrastructure.adapter.out.persistence;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private final ConcurrentMap<CategoryId, Category> store = new ConcurrentHashMap<>();

    @Override
    public void save(Category category) {
        store.put(category.id(), category);
    }

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        return Optional.ofNullable(store.get(categoryId));
    }

    @Override
    public List<Category> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean existsByName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return store.values().stream()
                .anyMatch(category -> category.name().toLowerCase(Locale.ROOT).equals(normalized));
    }
}
