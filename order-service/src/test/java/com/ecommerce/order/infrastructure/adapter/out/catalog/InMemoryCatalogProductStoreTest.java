package com.ecommerce.order.infrastructure.adapter.out.catalog;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCatalogProductStoreTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    private InMemoryCatalogProductStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryCatalogProductStore();
    }

    @Test
    void upsertsAndFindsProductScopedByCompany() {
        store.upsert(product(COMPANY, "Notebook", "1000.00"));

        Optional<CatalogProduct> found = store.findById(COMPANY, PRODUCT);

        assertThat(found).isPresent();
        assertThat(found.get().productName()).isEqualTo("Notebook");
    }

    @Test
    void sameProductIdInDifferentCompaniesAreIndependentSnapshots() {
        store.upsert(product(COMPANY, "Notebook", "1000.00"));
        store.upsert(product(OTHER_COMPANY, "Notebook Pro", "2000.00"));

        assertThat(store.findById(COMPANY, PRODUCT).orElseThrow().productName()).isEqualTo("Notebook");
        assertThat(store.findById(OTHER_COMPANY, PRODUCT).orElseThrow().productName()).isEqualTo("Notebook Pro");
    }

    @Test
    void findInCompanyWithoutSnapshotReturnsEmpty() {
        store.upsert(product(COMPANY, "Notebook", "1000.00"));

        assertThat(store.findById(OTHER_COMPANY, PRODUCT)).isEmpty();
    }

    @Test
    void upsertOverwritesExistingSnapshotForSameCompanyAndProduct() {
        store.upsert(product(COMPANY, "Notebook", "1000.00"));
        store.upsert(product(COMPANY, "Notebook", "800.00"));

        assertThat(store.findById(COMPANY, PRODUCT).orElseThrow().price().amount())
                .isEqualByComparingTo("800.00");
        assertThat(store.findAll()).hasSize(1);
    }

    private CatalogProduct product(CompanyId companyId, String name, String price) {
        return new CatalogProduct(companyId, PRODUCT, name,
                new Money(new BigDecimal(price), USD), CatalogProductStatus.ACTIVE, Instant.now());
    }
}
