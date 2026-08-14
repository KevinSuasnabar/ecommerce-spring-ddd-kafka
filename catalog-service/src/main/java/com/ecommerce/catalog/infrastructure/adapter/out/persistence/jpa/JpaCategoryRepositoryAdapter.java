package com.ecommerce.catalog.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpa;

    public JpaCategoryRepositoryAdapter(JpaCategoryRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(Category category) {
        jpa.save(new CategoryJpaEntity(
                category.id().value(),
                category.companyId().value(),
                category.name(),
                category.parentId() != null ? category.parentId().value() : null,
                category.createdAt(),
                category.updatedAt()));
    }

    @Override
    public Optional<Category> findById(CompanyId companyId, CategoryId categoryId) {
        return jpa.findById(categoryId.value())
                .filter(e -> e.getCompanyId().equals(companyId.value()))
                .map(this::toDomain);
    }

    @Override
    public List<Category> findAllByCompanyId(CompanyId companyId) {
        return jpa.findAllByCompanyId(companyId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(CompanyId companyId, String name) {
        return jpa.existsByCompanyIdAndNameIgnoreCase(companyId.value(), name);
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(
                new CategoryId(entity.getId()),
                new CompanyId(entity.getCompanyId()),
                entity.getName(),
                entity.getParentId() != null ? new CategoryId(entity.getParentId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
