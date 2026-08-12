package com.ecommerce.warehouse.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;


class StockIdTest {

    private static final ProductId PRODUCT_ID = new ProductId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000002"));

    @Test
    void rejectsNullCompanyId() {
        assertThatThrownBy(() -> new StockId(null, PRODUCT_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("company id must not be null");

    }

    @Test
    void rejectsNullProductId() {
        assertThatThrownBy(() -> new StockId(COMPANY_ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("product id must not be null");

    }

    @Test
    void equalWhenSameCompanyAndProduct() {
        var stockA = new StockId(COMPANY_ID, PRODUCT_ID);
        var stockB = new StockId(COMPANY_ID, PRODUCT_ID);
        assertThat(stockA).isEqualTo(stockB);
    }
}
