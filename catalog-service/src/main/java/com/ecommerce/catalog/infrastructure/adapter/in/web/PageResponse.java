package com.ecommerce.catalog.infrastructure.adapter.in.web;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
}
