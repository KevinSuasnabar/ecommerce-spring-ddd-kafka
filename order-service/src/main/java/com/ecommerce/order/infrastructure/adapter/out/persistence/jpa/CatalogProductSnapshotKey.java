package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CatalogProductSnapshotKey implements Serializable {

    private UUID productId;
    private UUID companyId;

    public CatalogProductSnapshotKey() {
    }

    public CatalogProductSnapshotKey(UUID productId, UUID companyId) {
        this.productId = productId;
        this.companyId = companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CatalogProductSnapshotKey other)) return false;
        return Objects.equals(productId, other.productId) && Objects.equals(companyId, other.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, companyId);
    }
}
