package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.ChangeProductPriceCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record ChangeProductPriceRequest(@NotNull @Positive BigDecimal price, @NotNull Currency currency) {

    public ChangeProductPriceCommand toCommand() {
        return new ChangeProductPriceCommand(price, currency);
    }
}
