package com.ecommerce.catalog.domain.exception;

import com.ecommerce.catalog.domain.model.CategoryId;

public class CategoryNotFoundException extends DomainException {

    public CategoryNotFoundException(CategoryId categoryId) {
        super("Category " + categoryId.value() + " not found");
    }
}
