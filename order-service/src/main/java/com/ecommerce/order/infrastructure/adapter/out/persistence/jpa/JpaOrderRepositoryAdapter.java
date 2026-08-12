package com.ecommerce.order.infrastructure.adapter.out.persistence.jpa;

import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderLine;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("postgres")
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpa;

    public JpaOrderRepositoryAdapter(JpaOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        entity.getLines().forEach(line -> line.setOrder(entity));
        jpa.save(entity);
    }

    @Override
    public Optional<Order> findById(CompanyId companyId, OrderId orderId) {
        return jpa.findByCompanyIdAndId(companyId.value(), orderId.value())
                .map(this::toDomain);
    }

    private OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                order.id().value(),
                order.customerId().value(),
                order.companyId().value(),
                order.status().name(),
                order.total().amount(),
                order.total().currency().getCurrencyCode(),
                order.shippingAddress().street(),
                order.shippingAddress().city(),
                order.shippingAddress().state(),
                order.shippingAddress().country(),
                order.shippingAddress().zipCode(),
                order.paymentMethod().name(),
                order.createdAt(),
                order.updatedAt());

        List<OrderLineJpaEntity> lineEntities = order.lines().stream()
                .map(line -> new OrderLineJpaEntity(
                        UUID.randomUUID(),
                        entity,
                        line.productId().value(),
                        line.productName(),
                        line.quantity(),
                        line.unitPrice().amount(),
                        line.unitPrice().currency().getCurrencyCode()))
                .toList();
        entity.setLines(lineEntities);
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(line -> new OrderLine(
                        new ProductId(line.getProductId()),
                        line.getProductName(),
                        line.getQuantity(),
                        new Money(line.getUnitPrice(), Currency.getInstance(line.getUnitCurrency()))))
                .toList();

        Address address = new Address(
                entity.getStreet(), entity.getCity(), entity.getState(),
                entity.getCountry(), entity.getZipCode());

        return Order.reconstitute(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                lines,
                address,
                PaymentMethod.valueOf(entity.getPaymentMethod()),
                OrderStatus.valueOf(entity.getStatus()),
                new CompanyId(entity.getCompanyId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
