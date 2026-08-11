package com.ecommerce.order.infrastructure.adapter.out.persistence;

import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderLine;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryOrderRepositoryTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final CompanyId OTHER_COMPANY = new CompanyId(UUID.fromString("80000000-0000-0000-0000-000000000002"));
    private static final CustomerId CUSTOMER = new CustomerId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final Address ADDRESS = new Address("Av. Siempre Viva 123", "Springfield", null, "AR", "1406");
    private static final ProductId PRODUCT = new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001"));

    private InMemoryOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Test
    void savesAndRetrievesOrder() {
        Order order = newOrder();
        repository.save(order);

        Optional<Order> found = repository.findById(COMPANY, order.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(order.id());
    }

    @Test
    void returnsEmptyForUnknownOrder() {
        assertThat(repository.findById(COMPANY, OrderId.newId())).isEmpty();
    }

    @Test
    void hidesOrderFromOtherCompany() {
        Order order = newOrder();
        repository.save(order);

        assertThat(repository.findById(OTHER_COMPANY, order.id())).isEmpty();
    }

    @Test
    void saveOverwritesExistingOrder() {
        Order order = newOrder();
        repository.save(order);
        order.confirm();
        repository.save(order);

        Optional<Order> found = repository.findById(COMPANY, order.id());

        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(com.ecommerce.order.domain.model.OrderStatus.CONFIRMED);
    }

    @Test
    void supportsConcurrentWrites() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<OrderId>> tasks = IntStream.range(0, 200)
                    .mapToObj(i -> (Callable<OrderId>) () -> {
                        Order order = newOrder();
                        repository.save(order);
                        return order.id();
                    })
                    .toList();

            List<OrderId> ids = executor.invokeAll(tasks).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            assertThat(ids).hasSize(200);
            assertThat(ids.stream().distinct()).hasSize(200);
        } finally {
            executor.shutdownNow();
        }
    }

    private Order newOrder() {
        OrderLine line = new OrderLine(PRODUCT, "Notebook", 1, new Money(new BigDecimal("1500.00"), USD));
        return Order.create(OrderId.newId(), CUSTOMER, List.of(line), ADDRESS, PaymentMethod.CREDIT_CARD, COMPANY);
    }
}
