package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Optional;

@Component
public class JpaCatalogProductStoreAdapter implements CatalogProductStore {

    private final JpaCatalogProductSnapshotRepository jpa;

    public JpaCatalogProductStoreAdapter(JpaCatalogProductSnapshotRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void upsert(CatalogProduct product) {
        CatalogProductSnapshotEntity entity = new CatalogProductSnapshotEntity(
                product.productId().value(),
                product.companyId().value(),
                product.productName(),
                product.price().amount(),
                product.price().currency().getCurrencyCode(),
                product.status().name(),
                product.updatedAt());
        jpa.save(entity);
    }

    @Override
    public Optional<CatalogProduct> findById(CompanyId companyId, ProductId productId) {
        return jpa.findByCompanyIdAndProductId(companyId.value(), productId.value())
                .map(this::toDomain);
    }

    private CatalogProduct toDomain(CatalogProductSnapshotEntity entity) {
        return new CatalogProduct(
                new CompanyId(entity.getCompanyId()),
                new ProductId(entity.getProductId()),
                entity.getProductName(),
                new Money(entity.getPrice(), Currency.getInstance(entity.getCurrency())),
                CatalogProductStatus.from(entity.getStatus()),
                entity.getUpdatedAt());
    }
}
