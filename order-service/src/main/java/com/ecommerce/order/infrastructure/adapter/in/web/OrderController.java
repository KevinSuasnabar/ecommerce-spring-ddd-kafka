package com.ecommerce.order.infrastructure.adapter.in.web;

import com.ecommerce.order.application.dto.CancelOrderCommand;
import com.ecommerce.order.application.port.in.CancelOrderUseCase;
import com.ecommerce.order.application.port.in.ConfirmOrderUseCase;
import com.ecommerce.order.application.port.in.CreateOrderUseCase;
import com.ecommerce.order.application.port.in.DeliverOrderUseCase;
import com.ecommerce.order.application.port.in.GetOrderUseCase;
import com.ecommerce.order.application.port.in.ShipOrderUseCase;
import com.ecommerce.order.domain.model.CompanyId;
import com.ecommerce.order.domain.model.OrderId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final String ORDERS_PATH = "/api/orders/";

    private final CreateOrderUseCase createOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final ShipOrderUseCase shipOrderUseCase;
    private final DeliverOrderUseCase deliverOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final CompanyContext companyContext;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           ConfirmOrderUseCase confirmOrderUseCase,
                           ShipOrderUseCase shipOrderUseCase,
                           DeliverOrderUseCase deliverOrderUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           GetOrderUseCase getOrderUseCase,
                           CompanyContext companyContext) {
        this.createOrderUseCase = createOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.shipOrderUseCase = shipOrderUseCase;
        this.deliverOrderUseCase = deliverOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.companyContext = companyContext;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        CompanyId companyId = companyContext.currentCompanyId();
        OrderId orderId = createOrderUseCase.createOrder(request.toCommand(companyId));
        return ResponseEntity
                .created(URI.create(ORDERS_PATH + orderId.value()))
                .body(CreateOrderResponse.from(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID orderId) {
        CompanyId companyId = companyContext.currentCompanyId();
        OrderResponse response = OrderResponse.from(getOrderUseCase.getOrder(companyId, new OrderId(orderId)));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable UUID orderId) {
        confirmOrderUseCase.confirmOrder(companyContext.currentCompanyId(), new OrderId(orderId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<Void> ship(@PathVariable UUID orderId) {
        shipOrderUseCase.shipOrder(companyContext.currentCompanyId(), new OrderId(orderId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<Void> deliver(@PathVariable UUID orderId) {
        deliverOrderUseCase.deliverOrder(companyContext.currentCompanyId(), new OrderId(orderId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        cancelOrderUseCase.cancelOrder(companyContext.currentCompanyId(),
                new CancelOrderCommand(new OrderId(orderId), request.reason()));
        return ResponseEntity.noContent().build();
    }
}
