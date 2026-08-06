package com.ecommerce.order.domain.exception;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
