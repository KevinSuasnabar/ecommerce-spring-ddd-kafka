package com.ecommerce.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void addsTwoAmountsWithSameCurrency() {
        Money result = new Money(new BigDecimal("10.00"), USD).add(new Money(new BigDecimal("5.50"), USD));

        assertThat(result.amount()).isEqualByComparingTo("15.50");
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    void rejectsAddingDifferentCurrencies() {
        Money usd = new Money(new BigDecimal("10.00"), USD);
        Money eur = new Money(new BigDecimal("10.00"), Currency.getInstance("EUR"));

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), USD))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
