package com.ecommerce.catalog.application.dto;

import java.util.List;

public record ProductPageResult(List<ProductSummary> items, int page, int size, long totalElements, int totalPages) {

    public static ProductPageResult of(List<ProductSummary> items, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new ProductPageResult(items, page, size, totalElements, totalPages);
    }
}
