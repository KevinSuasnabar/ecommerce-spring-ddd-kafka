package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CreateProductCommand;
import com.ecommerce.catalog.domain.model.CompanyId;
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

    public CreateProductCommand toCommand(CompanyId companyId) {
        return new CreateProductCommand(companyId, name, description, price, currency);
    }
}
