package com.ecommerce.warehouse.infrastructure.adapter.out.persistence;

import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStockRepositoryTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    private InMemoryStockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryStockRepository(new InMemoryStockLevelStore());
    }

    @Test
    void savesAndRetrievesStock() {
        Stock stock = newStock();
        repository.save(stock);

        Optional<Stock> found = repository.findById(stock.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(stock.id());
    }

    @Test
    void returnsEmptyForUnknownStock() {
        assertThat(repository.findById(newStock().id())).isEmpty();
    }

    @Test
    void saveOverwritesExistingStock() {
        Stock stock = newStock();
        repository.save(stock);
        stock.receive(new Quantity(10));
        repository.save(stock);

        Optional<Stock> found = repository.findById(stock.id());

        assertThat(found).isPresent();
        assertThat(found.get().available().value()).isEqualTo(10);
    }

    @Test
    void hidesStockFromOtherCompany() {
        StockId companyStock = new StockId(COMPANY, PRODUCT);
        Stock stock = Stock.create(companyStock);
        repository.save(stock);

        assertThat(repository.findById(new StockId(OTHER_COMPANY, PRODUCT))).isEmpty();
        assertThat(repository.findById(companyStock)).isPresent();
    }

    private Stock newStock() {
        return Stock.create(new StockId(COMPANY, PRODUCT));
    }
}
