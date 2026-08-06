package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.UpdateProductCommand;
import jakarta.validation.constraints.NotBlank;

public record UpdateProductRequest(@NotBlank String name, String description) {

    public UpdateProductCommand toCommand() {
        return new UpdateProductCommand(name, description);
    }
}
