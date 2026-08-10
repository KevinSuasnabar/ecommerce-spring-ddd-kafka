package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findById(CompanyId companyId, CategoryId categoryId);

    List<Category> findAllByCompanyId(CompanyId companyId);

    boolean existsByName(CompanyId companyId, String name);
}
