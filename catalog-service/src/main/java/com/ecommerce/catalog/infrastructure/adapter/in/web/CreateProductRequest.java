package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CreateProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotNull Currency currency) {

    public CreateProductCommand toCommand() {
        return new CreateProductCommand(name, description, price, currency);
    }
}
