package com.ecommerce.catalog.domain.model;

import com.ecommerce.catalog.domain.event.ProductActivatedEvent;
import com.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.ecommerce.catalog.domain.event.ProductPriceChangedEvent;
import com.ecommerce.catalog.domain.event.ProductRetiredEvent;
import com.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.ecommerce.catalog.domain.exception.InvalidProductTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void createBuildsDraftProductWithCreatedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", "16GB RAM", new Money(new BigDecimal("1500.00"), USD));

        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.name()).isEqualTo("Notebook");
        assertThat(product.categories()).isEmpty();
        assertThat(product.pullDomainEvents())
                .extracting(event -> event.getClass().getSimpleName())
                .containsExactly("ProductCreatedEvent");
    }

    @Test
    void createRejectsBlankNameAndNegativePrice() {
        assertThatThrownBy(() -> Product.create(ProductId.newId(), " ", "desc", new Money(BigDecimal.ONE, USD)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Product.create(ProductId.newId(), "Name", "desc", new Money(new BigDecimal("-1"), USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activateEmitsActivatedEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));

        product.activate();

        assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.pullDomainEvents())
                .anyMatch(event -> event instanceof ProductActivatedEvent);
    }

    @Test
    void retireFromDraftEmitsRetiredEvent() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));

        product.retire();

        assertThat(product.status()).isEqualTo(ProductStatus.RETIRED);
        assertThat(product.pullDomainEvents())
                .anyMatch(event -> event instanceof ProductRetiredEvent);
    }

    @Test
    void activatingAnActiveProductIsRejected() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.activate();
        product.pullDomainEvents();

        assertThatThrownBy(product::activate)
                .isInstanceOf(InvalidProductTransitionException.class);
    }

    @Test
    void retiredProductCannotBeUpdatedOrRepriced() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.retire();
        product.pullDomainEvents();

        assertThatThrownBy(() -> product.update("New name", null))
                .isInstanceOf(InvalidProductTransitionException.class);
        assertThatThrownBy(() -> product.changePrice(new Money(new BigDecimal("100.00"), USD)))
                .isInstanceOf(InvalidProductTransitionException.class);
    }

    @Test
    void changePriceEmitsPriceChangedEventWithOldAndNewPrice() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.pullDomainEvents();

        product.changePrice(new Money(new BigDecimal("1400.00"), USD));

        assertThat(product.price().amount()).isEqualByComparingTo("1400.00");
        assertThat(product.pullDomainEvents())
                .anyMatch(event -> event instanceof ProductPriceChangedEvent changed
                        && changed.oldPrice().amount().compareTo(new BigDecimal("1500.00")) == 0
                        && changed.newPrice().amount().compareTo(new BigDecimal("1400.00")) == 0);
    }

    @Test
    void updateChangesNameAndDescription() {
        Product product = Product.create(ProductId.newId(), "Notebook", "old", new Money(new BigDecimal("1500.00"), USD));
        product.pullDomainEvents();

        product.update("Notebook Pro", "new spec");

        assertThat(product.name()).isEqualTo("Notebook Pro");
        assertThat(product.description()).isEqualTo("new spec");
        assertThat(product.pullDomainEvents())
                .anyMatch(event -> event instanceof ProductUpdatedEvent);
    }

    @Test
    void assignAndRemoveCategory() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        CategoryId categoryId = CategoryId.newId();

        product.assignCategory(categoryId);
        assertThat(product.categories()).containsExactly(categoryId);

        product.removeCategory(categoryId);
        assertThat(product.categories()).isEmpty();
    }

    @Test
    void categoriesAreImmutableFromOutside() {
        Product product = Product.create(ProductId.newId(), "Notebook", null, new Money(new BigDecimal("1500.00"), USD));
        product.assignCategory(CategoryId.newId());

        assertThatThrownBy(() -> product.categories().add(CategoryId.newId()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
