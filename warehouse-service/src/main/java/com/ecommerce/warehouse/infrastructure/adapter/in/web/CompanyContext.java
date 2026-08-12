package com.ecommerce.warehouse.infrastructure.adapter.in.web;

import com.ecommerce.warehouse.domain.model.CompanyId;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CompanyContext {
    public CompanyId currentCompanyId() {
        // TODO: leer del SecurityContext (JWT) cuando exista auth real
        return new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    }
}
