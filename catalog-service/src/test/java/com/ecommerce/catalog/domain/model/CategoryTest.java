package com.ecommerce.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CategoryTest {

    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));

    @Test
    void createBuildsCategoryWithOptionalParent() {
        Category parent = Category.create(CategoryId.newId(), "Computers", null, COMPANY_ID);
        Category child = Category.create(CategoryId.newId(), "Laptops", parent.id(), COMPANY_ID);

        assertThat(child.name()).isEqualTo("Laptops");
        assertThat(child.parentId()).isEqualTo(parent.id());
        assertThat(parent.parentId()).isNull();
    }

    @Test
    void createCategoryWithCompanyId() {
        Category category = Category.create(CategoryId.newId(), "test-category", null, COMPANY_ID);

        assertThat(category.companyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    void createCategoryWithoutCompanyId() {
        assertThatThrownBy(() -> Category.create(CategoryId.newId(), "test-category", null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("company id");

    }


    @Test
    void renameUpdatesName() {
        Category category = Category.create(CategoryId.newId(), "Computers", null, COMPANY_ID);

        category.rename("Computing");

        assertThat(category.name()).isEqualTo("Computing");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Category.create(CategoryId.newId(), "  ", null, COMPANY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
