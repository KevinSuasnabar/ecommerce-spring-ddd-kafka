package com.ecommerce.catalog.application.dto;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateProductCommand(String name, String description, BigDecimal price, Currency currency) {
}
