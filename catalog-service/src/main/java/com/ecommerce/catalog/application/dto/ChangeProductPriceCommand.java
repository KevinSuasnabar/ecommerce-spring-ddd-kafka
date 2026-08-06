package com.ecommerce.catalog.application.dto;

import java.math.BigDecimal;
import java.util.Currency;

public record ChangeProductPriceCommand(BigDecimal price, Currency currency) {
}
