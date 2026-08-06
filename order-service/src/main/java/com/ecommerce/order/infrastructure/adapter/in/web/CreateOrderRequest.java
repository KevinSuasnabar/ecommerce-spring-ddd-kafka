package com.ecommerce.order.infrastructure.adapter.in.web;

import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty List<@Valid LineRequest> lines,
        @NotNull @Valid AddressRequest shippingAddress,
        @NotNull PaymentMethod paymentMethod) {

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(
                new CustomerId(customerId),
                shippingAddress.toDomain(),
                lines.stream().map(LineRequest::toCommand).toList(),
                paymentMethod);
    }

    public record LineRequest(
            @NotNull UUID productId,
            @Min(1) int quantity) {

        public CreateOrderCommand.OrderLineCommand toCommand() {
            return new CreateOrderCommand.OrderLineCommand(
                    new ProductId(productId),
                    quantity);
        }
    }

    public record AddressRequest(
            @NotBlank String street,
            @NotBlank String city,
            String state,
            @NotBlank String country,
            @NotBlank String zipCode) {

        public Address toDomain() {
            return new Address(street, city, state, country, zipCode);
        }
    }
}
