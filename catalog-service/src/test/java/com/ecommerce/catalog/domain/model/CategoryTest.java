package com.ecommerce.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createBuildsCategoryWithOptionalParent() {
        Category parent = Category.create(CategoryId.newId(), "Computers", null);
        Category child = Category.create(CategoryId.newId(), "Laptops", parent.id());

        assertThat(child.name()).isEqualTo("Laptops");
        assertThat(child.parentId()).isEqualTo(parent.id());
        assertThat(parent.parentId()).isNull();
    }

    @Test
    void renameUpdatesName() {
        Category category = Category.create(CategoryId.newId(), "Computers", null);

        category.rename("Computing");

        assertThat(category.name()).isEqualTo("Computing");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Category.create(CategoryId.newId(), "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
