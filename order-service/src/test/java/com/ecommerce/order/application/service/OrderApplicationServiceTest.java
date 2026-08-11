package com.ecommerce.order.application.service;

import com.ecommerce.order.application.dto.CancelOrderCommand;
import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.dto.CreateOrderCommand;
import com.ecommerce.order.application.dto.OrderQueryResult;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.application.port.out.EventPublisher;
import com.ecommerce.order.application.port.out.InventoryPort;
import com.ecommerce.order.application.port.out.NotificationPort;
import com.ecommerce.order.application.port.out.PaymentPort;
import com.ecommerce.order.application.port.out.PaymentResult;
import com.ecommerce.order.domain.event.OrderConfirmedEvent;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.exception.InsufficientStockException;
import com.ecommerce.order.domain.exception.InvalidOrderTransitionException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.exception.PaymentRejectedException;
import com.ecommerce.order.domain.exception.ProductNotAvailableException;
import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CatalogProductStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
    private static final CustomerId CUSTOMER = new CustomerId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final Address ADDRESS = new Address("Av. Siempre Viva 123", "Springfield", null, "AR", "1406");
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CatalogProductStore catalogProductStore;
    @Mock
    private PaymentPort paymentPort;
    @Mock
    private InventoryPort inventoryPort;
    @Mock
    private NotificationPort notificationPort;
    @Mock
    private EventPublisher eventPublisher;

    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OrderApplicationService(orderRepository, catalogProductStore, paymentPort, inventoryPort, notificationPort, eventPublisher);
    }

    @Test
    void createOrderPersistsAndPublishesCreatedEvent() {
        when(catalogProductStore.findById(COMPANY, PRODUCT)).thenReturn(Optional.of(activeCatalogProduct()));
        CreateOrderCommand command = validCreateCommand();

        OrderId orderId = service.createOrder(command);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrder.capture());
        assertThat(savedOrder.getValue().id()).isEqualTo(orderId);
        assertThat(savedOrder.getValue().companyId()).isEqualTo(COMPANY);
        assertThat(savedOrder.getValue().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.getValue().total().amount()).isEqualByComparingTo("3000.00");
        verify(eventPublisher).publish(any(OrderCreatedEvent.class));
        verify(notificationPort).orderStatusChanged(any(), any(), any());
    }

    @Test
    void createOrderWithUnknownProductRejectsWithoutSaving() {
        when(catalogProductStore.findById(COMPANY, PRODUCT)).thenReturn(Optional.empty());
        CreateOrderCommand command = validCreateCommand();

        assertThatThrownBy(() -> service.createOrder(command))
                .isInstanceOf(ProductNotAvailableException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createOrderWithProductFromAnotherCompanyIsRejected() {
        when(catalogProductStore.findById(COMPANY, PRODUCT)).thenReturn(Optional.empty());
        CreateOrderCommand command = validCreateCommand();

        assertThatThrownBy(() -> service.createOrder(command))
                .isInstanceOf(ProductNotAvailableException.class);

        verify(catalogProductStore, never()).findById(OTHER_COMPANY, PRODUCT);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderWithNonActiveProductRejectsWithoutSaving() {
        when(catalogProductStore.findById(COMPANY, PRODUCT))
                .thenReturn(Optional.of(new CatalogProduct(COMPANY, PRODUCT, "Notebook",
                        new Money(new BigDecimal("1500.00"), USD), CatalogProductStatus.DRAFT, Instant.now())));
        CreateOrderCommand command = validCreateCommand();

        assertThatThrownBy(() -> service.createOrder(command))
                .isInstanceOf(ProductNotAvailableException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void confirmOrderChargesPaymentAndTransitionsToConfirmed() {
        Order order = createdOrder();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));
        when(inventoryPort.reserve(any(), anyInt())).thenReturn(true);
        when(paymentPort.charge(any(), any())).thenReturn(PaymentResult.approved("CC-1"));

        service.confirmOrder(COMPANY, order.id());

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(paymentPort).charge(order.total(), PaymentMethod.CREDIT_CARD);
        verify(orderRepository).save(order);
        verify(eventPublisher).publish(any(OrderConfirmedEvent.class));
    }

    @Test
    void confirmOrderWithRejectedPaymentLeavesOrderCreated() {
        Order order = createdOrder();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));
        when(inventoryPort.reserve(any(), anyInt())).thenReturn(true);
        when(paymentPort.charge(any(), any())).thenReturn(PaymentResult.rejected());

        assertThatThrownBy(() -> service.confirmOrder(COMPANY, order.id()))
                .isInstanceOf(PaymentRejectedException.class);

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        verify(eventPublisher, never()).publish(any(OrderConfirmedEvent.class));
    }

    @Test
    void confirmOrderWithInsufficientStockSkipsPayment() {
        Order order = createdOrder();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));
        when(inventoryPort.reserve(any(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.confirmOrder(COMPANY, order.id()))
                .isInstanceOf(InsufficientStockException.class);

        verify(paymentPort, never()).charge(any(), any());
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void confirmOrderFromWrongStateIsRejectedBeforeExternalCalls() {
        Order order = createdOrder();
        order.confirm();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.confirmOrder(COMPANY, order.id()))
                .isInstanceOf(InvalidOrderTransitionException.class);

        verify(inventoryPort, never()).reserve(any(), anyInt());
        verify(paymentPort, never()).charge(any(), any());
    }

    @Test
    void shipOrderTransitionsToShipped() {
        Order order = createdOrder();
        order.confirm();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));

        service.shipOrder(COMPANY, order.id());

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderTransitionsToCancelledWithReason() {
        Order order = createdOrder();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));

        service.cancelOrder(COMPANY, new CancelOrderCommand(order.id(), "changed my mind"));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderReturnsQueryResult() {
        Order order = createdOrder();
        when(orderRepository.findById(COMPANY, order.id())).thenReturn(Optional.of(order));

        OrderQueryResult result = service.getOrder(COMPANY, order.id());

        assertThat(result.id()).isEqualTo(order.id());
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.lines()).hasSize(1);
    }

    @Test
    void getOrderThatDoesNotExistThrows() {
        OrderId unknown = OrderId.newId();
        when(orderRepository.findById(COMPANY, unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(COMPANY, unknown))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderFromAnotherCompanyThrowsNotFound() {
        Order order = createdOrder();
        when(orderRepository.findById(OTHER_COMPANY, order.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(OTHER_COMPANY, order.id()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private CreateOrderCommand validCreateCommand() {
        return new CreateOrderCommand(
                COMPANY,
                CUSTOMER,
                ADDRESS,
                List.of(new CreateOrderCommand.OrderLineCommand(PRODUCT, 2)),
                PaymentMethod.CREDIT_CARD);
    }

    private CatalogProduct activeCatalogProduct() {
        return new CatalogProduct(COMPANY, PRODUCT, "Notebook",
                new Money(new BigDecimal("1500.00"), USD), CatalogProductStatus.ACTIVE, Instant.now());
    }

    private Order createdOrder() {
        OrderLine line = new OrderLine(PRODUCT, "Notebook", 2, new Money(new BigDecimal("1500.00"), USD));
        Order order = Order.create(OrderId.newId(), CUSTOMER, List.of(line), ADDRESS, PaymentMethod.CREDIT_CARD, COMPANY);
        order.pullDomainEvents();
        return order;
    }
}
