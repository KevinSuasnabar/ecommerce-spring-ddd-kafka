package com.ecommerce.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void addsTwoAmountsWithSameCurrency() {
        Money a = new Money(new BigDecimal("10.00"), USD);
        Money b = new Money(new BigDecimal("5.50"), USD);

        Money result = a.add(b);

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
    void multipliesByQuantity() {
        Money unitPrice = new Money(new BigDecimal("12.99"), USD);

        Money result = unitPrice.times(3);

        assertThat(result.amount()).isEqualByComparingTo("38.97");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), USD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeMultiplier() {
        Money money = new Money(new BigDecimal("10.00"), USD);

        assertThatThrownBy(() -> money.times(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroIsAllowed() {
        Money zero = Money.zero(USD);

        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
