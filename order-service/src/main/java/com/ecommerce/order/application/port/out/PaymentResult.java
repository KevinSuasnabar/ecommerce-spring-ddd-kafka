package com.ecommerce.order.application.port.out;

public record PaymentResult(boolean approved, String authorizationId) {

    public static PaymentResult approved(String authorizationId) {
        return new PaymentResult(true, authorizationId);
    }

    public static PaymentResult rejected() {
        return new PaymentResult(false, null);
    }
}
