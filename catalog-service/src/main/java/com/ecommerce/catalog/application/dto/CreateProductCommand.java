package com.ecommerce.catalog.application.dto;

import com.ecommerce.catalog.domain.model.CompanyId;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateProductCommand(CompanyId companyId, String name, String description, BigDecimal price,
                                   Currency currency) {
}
