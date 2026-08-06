package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findById(CategoryId categoryId);

    List<Category> findAll();

    boolean existsByName(String name);
}
