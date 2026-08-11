package com.ecommerce.order.application.service;

import com.ecommerce.order.application.dto.CancelOrderCommand;
import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderQueryResult;
import com.ecommerce.order.application.port.in.CancelOrderUseCase;
import com.ecommerce.order.application.port.in.ConfirmOrderUseCase;
import com.ecommerce.order.application.port.in.CreateOrderUseCase;
import com.ecommerce.order.application.port.in.DeliverOrderUseCase;
import com.ecommerce.order.application.port.in.GetOrderUseCase;
import com.ecommerce.order.application.port.in.ShipOrderUseCase;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.application.port.out.EventPublisher;
import com.ecommerce.order.application.port.out.InventoryPort;
import com.ecommerce.order.application.port.out.NotificationPort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.exception.InsufficientStockException;
import com.ecommerce.order.domain.exception.InvalidOrderTransitionException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.exception.PaymentRejectedException;
import com.ecommerce.order.domain.exception.ProductNotAvailableException;
import com.ecommerce.order.domain.event.DomainEvent;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderLine;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderApplicationService
        implements CreateOrderUseCase, ConfirmOrderUseCase, ShipOrderUseCase,
        DeliverOrderUseCase, CancelOrderUseCase, GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final CatalogProductStore catalogProductStore;
    private final PaymentPort paymentPort;
    private final InventoryPort inventoryPort;
    private final NotificationPort notificationPort;
    private final EventPublisher eventPublisher;

    public OrderApplicationService(OrderRepository orderRepository,
                                   CatalogProductStore catalogProductStore,
                                   PaymentPort paymentPort,
                                   InventoryPort inventoryPort,
                                   NotificationPort notificationPort,
                                   EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.catalogProductStore = catalogProductStore;
        this.paymentPort = paymentPort;
        this.inventoryPort = inventoryPort;
        this.notificationPort = notificationPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderId createOrder(CreateOrderCommand command) {
        CompanyId companyId = command.companyId();
        List<OrderLine> lines = command.lines().stream()
                .map(line -> {
                    CatalogProduct product = catalogProductStore.findById(companyId, line.productId())
                            .filter(CatalogProduct::canBeOrdered)
                            .orElseThrow(() -> new ProductNotAvailableException(line.productId()));
                    return new OrderLine(
                            product.productId(),
                            product.productName(),
                            line.quantity(),
                            product.price());
                })
                .toList();

        Order order = Order.create(
                OrderId.newId(),
                command.customerId(),
                lines,
                command.shippingAddress(),
                command.paymentMethod(),
                companyId);

        orderRepository.save(order);
        publishEvents(order);
        notificationPort.orderStatusChanged(order.id(), order.status(), order.customerId());
        return order.id();
    }

    @Override
    public void confirmOrder(CompanyId companyId, OrderId orderId) {
        Order order = loadOrder(companyId, orderId);
        ensureTransitionAllowed(order, OrderStatus.CONFIRMED);

        reserveInventory(order);
        PaymentResult payment = paymentPort.charge(order.total(), order.paymentMethod());
        if (!payment.approved()) {
            throw new PaymentRejectedException(order.id());
        }

        order.confirm();
        orderRepository.save(order);
        publishEvents(order);
        notificationPort.orderStatusChanged(order.id(), order.status(), order.customerId());
    }

    @Override
    public void shipOrder(CompanyId companyId, OrderId orderId) {
        Order order = loadOrder(companyId, orderId);
        order.ship();
        orderRepository.save(order);
        publishEvents(order);
        notificationPort.orderStatusChanged(order.id(), order.status(), order.customerId());
    }

    @Override
    public void deliverOrder(CompanyId companyId, OrderId orderId) {
        Order order = loadOrder(companyId, orderId);
        order.deliver();
        orderRepository.save(order);
        publishEvents(order);
        notificationPort.orderStatusChanged(order.id(), order.status(), order.customerId());
    }

    @Override
    public void cancelOrder(CompanyId companyId, CancelOrderCommand command) {
        Order order = loadOrder(companyId, command.orderId());
        order.cancel(command.reason());
        orderRepository.save(order);
        publishEvents(order);
        notificationPort.orderStatusChanged(order.id(), order.status(), order.customerId());
    }

    @Override
    public OrderQueryResult getOrder(CompanyId companyId, OrderId orderId) {
        return OrderQueryResult.from(loadOrder(companyId, orderId));
    }

    private Order loadOrder(CompanyId companyId, OrderId orderId) {
        return orderRepository.findById(companyId, orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void ensureTransitionAllowed(Order order, OrderStatus target) {
        if (!order.status().canTransitionTo(target)) {
            throw new InvalidOrderTransitionException(order.id(), order.status(), target);
        }
    }

    private void reserveInventory(Order order) {
        for (OrderLine line : order.lines()) {
            if (!inventoryPort.reserve(line.productId(), line.quantity())) {
                throw new InsufficientStockException(order.id(), line.productId());
            }
        }
    }

    private void publishEvents(Order order) {
        for (DomainEvent event : order.pullDomainEvents()) {
            eventPublisher.publish(event);
        }
    }
}
