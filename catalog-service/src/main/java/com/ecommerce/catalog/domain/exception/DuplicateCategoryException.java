package com.ecommerce.catalog.domain.exception;

public class DuplicateCategoryException extends DomainException {

    public DuplicateCategoryException(String name) {
        super("Category '" + name + "' already exists");
    }
}
