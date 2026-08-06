package com.ecommerce.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStatusTest {

    @Test
    void draftCanBeActivatedOrRetired() {
        assertThat(ProductStatus.DRAFT.canTransitionTo(ProductStatus.ACTIVE)).isTrue();
        assertThat(ProductStatus.DRAFT.canTransitionTo(ProductStatus.RETIRED)).isTrue();
    }

    @Test
    void activeCanBeRetired() {
        assertThat(ProductStatus.ACTIVE.canTransitionTo(ProductStatus.RETIRED)).isTrue();
        assertThat(ProductStatus.ACTIVE.canTransitionTo(ProductStatus.DRAFT)).isFalse();
    }

    @Test
    void retiredIsTerminal() {
        assertThat(ProductStatus.RETIRED.canTransitionTo(ProductStatus.ACTIVE)).isFalse();
        assertThat(ProductStatus.RETIRED.canTransitionTo(ProductStatus.DRAFT)).isFalse();
        assertThat(ProductStatus.RETIRED.canTransitionTo(ProductStatus.RETIRED)).isFalse();
    }
}
